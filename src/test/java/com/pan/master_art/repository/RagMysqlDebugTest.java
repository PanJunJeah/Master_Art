package com.pan.master_art.repository;

import com.pan.master_art.entity.Artwork;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

/**
 * 真库诊断测试（可选，默认跳过）：
 * 验证 RAG 召回查询在真实 MySQL 上的行为（含 fetch join 后实体脱离事务访问 artist 是否触发懒加载）。
 *
 * 运行方式：
 *   mvn test -Dtest=RagMysqlDebugTest -Ddb.live=true \
 *     -Dspring.datasource.url="jdbc:mysql://localhost:3306/master_art?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai" \
 *     -Dspring.datasource.username=root -Dspring.datasource.password=123456 \
 *     -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
 *     -Dspring.jpa.hibernate.ddl-auto=none
 */
@Tag("db-live")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RagMysqlDebugTest {

    @Autowired
    private ArtworkRepository artworkRepository;

    @Test
    @DisplayName("db-live) 真库：findRagCandidates(星空) 应命中且 artist 可安全访问")
    void ragCandidatesAgainstRealMysql() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getProperty("db.live")),
                "db-live 测试默认跳过（-Ddb.live=true 启用）");

        List<Artwork> list = artworkRepository.findRagCandidates("星空");
        System.out.println("[RAG-DEBUG] findRagCandidates(星空) size=" + list.size());
        for (Artwork a : list) {
            System.out.println("[RAG-DEBUG] id=" + a.getId() + " name=" + a.getName()
                    + " artist=" + (a.getArtist() == null ? "null" : a.getArtist().getName()));
        }

        // 直连 JDBC 对比：中文参数绑定是否被字符集问题破坏
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/master_art?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                "root", "123456")) {
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM artworks WHERE story LIKE ?")) {
                ps.setString(1, "%星空%");
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("[RAG-DEBUG] JDBC story LIKE '%星空%' count=" + rs.getInt(1));
                }
            }
            try (java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM artworks WHERE story LIKE '%星空%'")) {
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("[RAG-DEBUG] JDBC literal LIKE count=" + rs.getInt(1));
                }
            }
        }

        // 事务已提交后访问 artist（复现工具线程中的访问方式）
        for (Artwork a : list) {
            if (a.getArtist() != null) {
                System.out.println("[RAG-DEBUG] artist name outside tx: " + a.getArtist().getName());
            }
        }
    }
}

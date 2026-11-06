package com.pan.master_art.service;

import com.pan.master_art.dto.SubscriptionView;
import com.pan.master_art.entity.Exhibition;
import com.pan.master_art.entity.ExhibitionSubscription;
import com.pan.master_art.repository.ExhibitionRepository;
import com.pan.master_art.repository.ExhibitionSubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 订阅视图组装测试：
 * 「我的订阅」页返回事务内组装的 SubscriptionView，杜绝 Jackson 序列化时的懒加载异常。
 */
class SubscriptionViewTest {

    @Test
    @DisplayName("订阅列表以视图返回，展览字段在服务层组装完成")
    void listViewsAssemblesInsideService() {
        ExhibitionSubscriptionRepository repo = mock(ExhibitionSubscriptionRepository.class);
        ExhibitionRepository exhibitionRepo = mock(ExhibitionRepository.class);
        SubscriptionService service = new SubscriptionService(repo, exhibitionRepo);

        Exhibition e = new Exhibition();
        e.setId(1L);
        e.setName("印象派大师展");
        e.setTheme("印象派");
        e.setLocation("上海·西岸美术馆");
        e.setStartTime(new Date(System.currentTimeMillis() + 86400_000L));

        ExhibitionSubscription s = new ExhibitionSubscription();
        s.setId(9L);
        s.setUserId(42L);
        s.setExhibition(e);
        s.setStatus(1);
        s.setSubscribedAt(new Date());

        when(repo.findByUserIdAndStatusOrderBySubscribedAtDesc(42L, 1)).thenReturn(List.of(s));

        List<SubscriptionView> views = service.listViewsByUser(42L);

        assertEquals(1, views.size());
        assertEquals(9L, views.get(0).getSubscriptionId());
        assertEquals(1L, views.get(0).getExhibitionId());
        assertEquals("印象派大师展", views.get(0).getExhibitionName());
        assertEquals("上海·西岸美术馆", views.get(0).getLocation());
        assertNotNull(views.get(0).getStartTime(), "开展时间应在服务层完成格式化");
    }
}

-- ============================================================
-- MasterArt AI 策展「试运行」种子数据（可重复执行，幂等）
-- 执行方式：
--   mysql -uroot -p123456 --default-character-set=utf8mb4 < trial-data.sql
-- 内容：启用艺术家 / 命名并上架艺术品（含 RAG 故事）/
--       修正展览时间与状态 / 插入票务场次 / 创建 demo 账号
-- ============================================================
USE master_art;

-- ===== 1. 艺术家：启用并补全资料 =====
UPDATE artists SET
  status = 1,
  english_name = IF(english_name IS NULL OR english_name = '',
      CASE id WHEN 1 THEN 'Leonardo da Vinci' WHEN 2 THEN 'Vincent van Gogh'
              WHEN 3 THEN 'Claude Monet' WHEN 4 THEN 'Pablo Picasso'
              WHEN 5 THEN 'Yayoi Kusama' END, english_name),
  genre = IF(genre IS NULL OR genre = '',
      CASE id WHEN 1 THEN '文艺复兴' WHEN 2 THEN '后印象派' WHEN 3 THEN '印象派'
              WHEN 4 THEN '立体主义' WHEN 5 THEN '波普/当代' END, genre),
  nationality = IF(nationality IS NULL OR nationality = '',
      CASE id WHEN 1 THEN '意大利' WHEN 2 THEN '荷兰' WHEN 3 THEN '法国'
              WHEN 4 THEN '西班牙' WHEN 5 THEN '日本' END, nationality),
  biography = IF(biography IS NULL OR biography = '',
      CASE id WHEN 1 THEN '文艺复兴三杰之一，画家、科学家与发明家。'
              WHEN 2 THEN '后印象派巨匠，以强烈的色彩与笔触著称。'
              WHEN 3 THEN '印象派代表人物，一生痴迷于光影与水面的变化。'
              WHEN 4 THEN '立体主义创始人，20 世纪最具影响力的艺术家之一。'
              WHEN 5 THEN '日本当代艺术家，以波点与南瓜闻名于世。' END, biography),
  photo_url = IF(photo_url IS NULL OR photo_url = '', CONCAT('https://picsum.photos/seed/artist', id, '/400/500'), photo_url),
  update_time = NOW()
WHERE id IN (1, 2, 3, 4, 5);

-- ===== 2. 艺术品：从 legacy title 迁移名称并上架 + 填充 RAG 知识字段 =====
UPDATE artworks SET
  name = title,
  english_name = CASE id WHEN 1 THEN 'Mona Lisa' WHEN 2 THEN 'The Starry Night'
                         WHEN 3 THEN 'Water Lilies' WHEN 4 THEN 'Les Demoiselles d''Avignon'
                         WHEN 5 THEN 'Pumpkin' END,
  genre = CASE id WHEN 1 THEN '文艺复兴' WHEN 2 THEN '后印象派' WHEN 3 THEN '印象派'
                  WHEN 4 THEN '立体主义' WHEN 5 THEN '波普艺术' END,
  creation_year = CASE id WHEN 1 THEN '1503' WHEN 2 THEN '1889' WHEN 3 THEN '1919'
                          WHEN 4 THEN '1907' WHEN 5 THEN '2016' END,
  type = 1, status = 1, weight = 100,
  material = CASE id WHEN 1 THEN '木板油画' WHEN 2 THEN '布面油画' WHEN 3 THEN '布面油画'
                     WHEN 4 THEN '布面油画' WHEN 5 THEN '综合材料' END,
  size = CASE id WHEN 1 THEN '77×53cm' WHEN 2 THEN '73.7×92.1cm' WHEN 3 THEN '200×425cm'
                 WHEN 4 THEN '243.9×233.7cm' WHEN 5 THEN '180×180cm' END,
  collection_place = CASE id WHEN 1 THEN '巴黎卢浮宫' WHEN 2 THEN '纽约现代艺术博物馆'
                             WHEN 3 THEN '巴黎橘园美术馆' WHEN 4 THEN '纽约现代艺术博物馆'
                             WHEN 5 THEN '直岛·倍乐生艺术场' END,
  description = CASE id WHEN 1 THEN '达·芬奇最著名的肖像画，神秘的微笑与晕涂法的典范。'
                        WHEN 2 THEN '梵高在圣雷米疗养院创作的夜景，漩涡状的星空与燃烧的柏树。'
                        WHEN 3 THEN '莫奈晚年在吉维尼花园创作的睡莲系列巨作之一，捕捉水面光影。'
                        WHEN 4 THEN '毕加索立体主义的开山之作，五位少女的几何化形体。'
                        WHEN 5 THEN '草间弥生标志性的波点南瓜雕塑，无限重复的视觉语言。' END,
  story = CASE id WHEN 1 THEN '创作于1503年前后，画中女子身份众说纷纭，其微笑成为艺术史上最著名的谜团之一，现藏于巴黎卢浮宫。'
                  WHEN 2 THEN '1889年梵高在圣雷米精神病院疗养期间，透过铁窗仰望夜空所绘。画中漩涡般的星空被认为表现了他内心的激荡。'
                  WHEN 3 THEN '1919年前后莫奈视力衰退，仍坚持在吉维尼花园绘制巨幅睡莲，希望用画面包围观众，营造沉浸式的光影体验。'
                  WHEN 4 THEN '1907年完成的《亚威农少女》颠覆了传统透视，五位女性形象融入非洲面具元素，被视为立体主义的起点。'
                  WHEN 5 THEN '草间弥生自幼受幻觉困扰，波点是她的标志语言。巨大的南瓜雕塑立在直岛海边，成为当代艺术的地标。' END,
  historical_significance = CASE id WHEN 1 THEN '文艺复兴人文主义的巅峰之作，影响此后五百年的肖像画传统。'
                                     WHEN 2 THEN '后印象派表现性笔触的极致，20世纪表现主义的重要先声。'
                                     WHEN 3 THEN '印象派对光色探索的集大成，启发了后来的抽象表现主义。'
                                     WHEN 4 THEN '立体主义诞生的宣言，彻底改写 20 世纪艺术的观看方式。'
                                     WHEN 5 THEN '波普与当代艺术融合的代表，装置与雕塑进入公共空间的典范。' END,
  features = CASE id WHEN 1 THEN '晕涂法；金字塔式构图；神秘的微笑'
                     WHEN 2 THEN '漩涡笔触；强烈补色；表现性星空'
                     WHEN 3 THEN '光影色斑；水面倒影；大尺幅沉浸'
                     WHEN 4 THEN '几何化形体；多视点；非洲面具元素'
                     WHEN 5 THEN '波点重复；高饱和黄色；公共雕塑' END,
  main_image_url = IF(main_image_url IS NULL OR main_image_url = '',
      CONCAT('https://picsum.photos/seed/artwork', id, '/800/600'), main_image_url),
  view_count = CASE id WHEN 1 THEN 1280 WHEN 2 THEN 2350 WHEN 3 THEN 1780 WHEN 4 THEN 1420 WHEN 5 THEN 980 END,
  update_time = NOW()
WHERE id IN (1, 2, 3, 4, 5);

-- ===== 3. 展览：命名 / 主题 / 时间 / 状态修正（相对今天） =====
UPDATE exhibitions SET
  name = CASE id WHEN 1 THEN '印象派大师展' WHEN 2 THEN '梵高回顾展' WHEN 3 THEN '当代艺术新视野'
                 WHEN 4 THEN '文艺复兴珍品展' WHEN 5 THEN '数字艺术展' END,
  theme = CASE id WHEN 1 THEN '印象派·后印象派' WHEN 2 THEN '后印象派' WHEN 3 THEN '当代艺术'
                  WHEN 4 THEN '文艺复兴' WHEN 5 THEN '数字艺术' END,
  location = CASE id WHEN 1 THEN '上海·西岸美术馆' WHEN 2 THEN '北京·尤伦斯当代艺术中心'
                     WHEN 3 THEN '深圳·海上世界文化艺术中心' WHEN 4 THEN '上海博物馆'
                     WHEN 5 THEN '杭州·天目里美术馆' END,
  curator = CASE id WHEN 1 THEN '王策展人' WHEN 2 THEN '李策展人' WHEN 3 THEN '张策展人'
                    WHEN 4 THEN '陈策展人' WHEN 5 THEN '刘策展人' END,
  description = CASE id WHEN 1 THEN '汇聚莫奈、梵高等大师真迹，一次看尽印象派与后印象派的光色革命。'
                        WHEN 2 THEN '全景回顾梵高一生的创作历程，展出《星夜》《向日葵》等名作。'
                        WHEN 3 THEN '从立体主义到数字媒介，呈现当代艺术的多元面貌。'
                        WHEN 4 THEN '文艺复兴巨匠原作集结，感受人文主义的光辉。'
                        WHEN 5 THEN '用算法与屏幕重新定义艺术，沉浸式数字艺术体验。' END,
  visit_info = '开放时间 10:00-18:00，周一闭馆，凭票入场',
  view_count = CASE id WHEN 1 THEN 3450 WHEN 2 THEN 2890 WHEN 3 THEN 2100 WHEN 4 THEN 3200 WHEN 5 THEN 1900 END,
  weight = 100,
  start_time = CASE id WHEN 1 THEN DATE_SUB(NOW(), INTERVAL 10 DAY)
                       WHEN 2 THEN DATE_SUB(NOW(), INTERVAL 5 DAY)
                       WHEN 3 THEN DATE_ADD(NOW(), INTERVAL 15 DAY)
                       WHEN 4 THEN DATE_ADD(NOW(), INTERVAL 30 DAY)
                       WHEN 5 THEN DATE_SUB(NOW(), INTERVAL 40 DAY) END,
  end_time = CASE id WHEN 1 THEN DATE_ADD(NOW(), INTERVAL 20 DAY)
                     WHEN 2 THEN DATE_ADD(NOW(), INTERVAL 25 DAY)
                     WHEN 3 THEN DATE_ADD(NOW(), INTERVAL 45 DAY)
                     WHEN 4 THEN DATE_ADD(NOW(), INTERVAL 60 DAY)
                     WHEN 5 THEN DATE_SUB(NOW(), INTERVAL 10 DAY) END,
  status = CASE id WHEN 1 THEN 'ONGOING' WHEN 2 THEN 'ONGOING' WHEN 3 THEN 'UPCOMING'
                   WHEN 4 THEN 'UPCOMING' WHEN 5 THEN 'ENDED' END,
  poster_url = IF(poster_url IS NULL OR poster_url = '', CONCAT('https://picsum.photos/seed/exhibition', id, '/1200/600'), poster_url),
  update_time = NOW()
WHERE id IN (1, 2, 3, 4, 5);

-- ===== 4. 票务场次（幂等：仅在空表时插入） =====
INSERT INTO ticket_sessions (exhibition_id, session_time, total_tickets, sold_tickets, price, status, create_time, update_time)
SELECT * FROM (
  SELECT 1 ex, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 10 HOUR st, 100 tt, 38 sld, 88.00 p, 1 s, NOW() ct, NOW() ut
  UNION ALL SELECT 1, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 14 HOUR, 100, 12, 88.00, 1, NOW(), NOW()
  UNION ALL SELECT 1, DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 10 HOUR, 80, 0, 128.00, 1, NOW(), NOW()
  UNION ALL SELECT 2, DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 10 HOUR, 120, 55, 98.00, 1, NOW(), NOW()
  UNION ALL SELECT 2, DATE_ADD(CURDATE(), INTERVAL 3 DAY) + INTERVAL 15 HOUR, 120, 3, 98.00, 1, NOW(), NOW()
  UNION ALL SELECT 3, DATE_ADD(CURDATE(), INTERVAL 16 DAY) + INTERVAL 10 HOUR, 100, 0, 68.00, 1, NOW(), NOW()
  UNION ALL SELECT 4, DATE_ADD(CURDATE(), INTERVAL 31 DAY) + INTERVAL 10 HOUR, 100, 0, 78.00, 1, NOW(), NOW()
) t
WHERE NOT EXISTS (SELECT 1 FROM ticket_sessions);

-- ===== 5. 试运行账号 demo / 123456（幂等） =====
INSERT INTO users (username, password, email, role, status, create_time, update_time)
SELECT 'demo', '123456', 'demo@masterart.com', 'USER', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'demo');

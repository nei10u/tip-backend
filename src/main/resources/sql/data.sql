-- 初始化 Banner 数据
-- 使用真实的电商风格 Banner 图片
INSERT INTO home_banner (title, image_url, link_url, sort_order, status) VALUES
('双11预售', 'https://img.alicdn.com/imgextra/i2/O1CN01yW8W8W1W1W1W1W1W1_!!6000000001000-0-tps-1125-350.jpg', 'https://s.click.taobao.com/test1', 1, 1),
('数码家电', 'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000002000-0-tps-1125-350.jpg', 'https://s.click.taobao.com/test2', 2, 1),
('美妆护肤', 'https://img.alicdn.com/imgextra/i3/O1CN01W1W1W1W1W1W1W1W1W_!!6000000003000-0-tps-1125-350.jpg', 'https://s.click.taobao.com/test3', 3, 1);

-- 初始化 Menu 数据
-- 使用标准分类图标
INSERT INTO home_menu (label, icon_url, link_url, type, sort_order, status) VALUES
('淘宝', 'https://gw.alicdn.com/tfs/TB1Wxi2trsrBKNjSZFpXXcXhFXa-183-144.png', '/pages/taobao', 'route', 1, 1),
('京东', 'https://gw.alicdn.com/tfs/TB10uhdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/jd', 'route', 2, 1),
('唯品会', 'https://gw.alicdn.com/tfs/TB11.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/vip', 'route', 3, 1),
('拼多多', 'https://gw.alicdn.com/tfs/TB13.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/pdd', 'route', 4, 1),
('抖音', 'https://gw.alicdn.com/tfs/TB15.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/douyin', 'route', 5, 1),
('苏宁', 'https://gw.alicdn.com/tfs/TB17.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/suning', 'route', 6, 1),
('聚划算', 'https://gw.alicdn.com/tfs/TB19.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/ju', 'route', 7, 1),
('天猫国际', 'https://gw.alicdn.com/tfs/TB1b.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/tmall', 'route', 8, 1),
('饿了么', 'https://gw.alicdn.com/tfs/TB1d.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/ele', 'route', 9, 1),
('更多', 'https://gw.alicdn.com/tfs/TB1f.hdtOqaBuNjt_iXxxxT2VXa-183-144.png', '/pages/more', 'route', 10, 1);

-- 初始化 Section 数据
INSERT INTO home_section (title, image_url, link_url, provider, sort_order, status) VALUES
('限时秒杀', 'https://img10.360buyimg.com/imagetools/jfs/t1/136434/12/1109/72108/5ed60812E64caf610/9d92c6c4bd412917.jpg!q70.webp', 'https://u.jd.com/EgLudCs', 'jd', 1, 1),
('百亿补贴', 'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000002000-0-tps-500-250.jpg', 'https://ju.taobao.com', 'taobao', 2, 1);

-- 初始化 Notice 数据
INSERT INTO home_notice (content, link_url, sort_order, status) VALUES
('双11超级红包已发放，点击领取！', 'https://s.click.taobao.com/redpacket', 1, 1),
('新人首单0元购，限时3天', 'https://s.click.taobao.com/newuser', 2, 1);

-- ============================================================
-- 京东 /pages/jd：顶部活动板块 & “更多”活动列表（ds_config_activity）
--
-- 前端读取规则（已实现）：
-- - status=1 AND support_banner=true AND support_app=true
-- - banner: 图片链接
-- - title: display_name
-- - rule: JSON 文本，字段自动识别：clickURL / shortURL / jdAppUrl
--
-- 说明：
-- - act_id 为业务主键（UNIQUE），这里用 ON CONFLICT 保证可重复执行。
-- - 你可以把下面的 clickURL/shortURL/jdAppUrl 替换成你准备好的真实链接。
-- ============================================================
INSERT INTO ds_config_activity (
  act_id,
  display_name,
  banner,
  path,
  jump_type,
  support_banner,
  support_app,
  support_mini,
  app_to_mini,
  status,
  scale,
  rule
) VALUES (
  10001,
  '国家补贴x百亿补贴',
  'https://img.alicdn.com/imgextra/i2/O1CN01W1W1W1W1W1W1W1W1W_!!6000000001000-0-tps-1125-350.jpg',
  'https://u.jd.com/EgtBMrL',
  'jd',
  TRUE,
  TRUE,
  FALSE,
  FALSE,
  1,
  100,
  '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'
) ON CONFLICT (act_id) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  banner = EXCLUDED.banner,
  path = EXCLUDED.path,
  jump_type = EXCLUDED.jump_type,
  support_banner = EXCLUDED.support_banner,
  support_app = EXCLUDED.support_app,
  support_mini = EXCLUDED.support_mini,
  app_to_mini = EXCLUDED.app_to_mini,
  status = EXCLUDED.status,
  scale = EXCLUDED.scale,
  rule = EXCLUDED.rule;

INSERT INTO ds_config_activity (
  act_id,
  display_name,
  banner,
  path,
  jump_type,
  support_banner,
  support_app,
  support_mini,
  app_to_mini,
  status,
  scale,
  rule
) VALUES (
  10002,
  '京东活动-示例2',
  'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000002000-0-tps-1125-350.jpg',
  'https://u.jd.com/EgtBMrL',
  'jd',
  TRUE,
  TRUE,
  FALSE,
  FALSE,
  1,
  90,
  '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'
) ON CONFLICT (act_id) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  banner = EXCLUDED.banner,
  path = EXCLUDED.path,
  jump_type = EXCLUDED.jump_type,
  support_banner = EXCLUDED.support_banner,
  support_app = EXCLUDED.support_app,
  support_mini = EXCLUDED.support_mini,
  app_to_mini = EXCLUDED.app_to_mini,
  status = EXCLUDED.status,
  scale = EXCLUDED.scale,
  rule = EXCLUDED.rule;

-- 更多占位活动（10003~10012）：方便你快速填充“更多”活动页
INSERT INTO ds_config_activity (
  act_id, display_name, banner, path, jump_type,
  support_banner, support_app, support_mini, app_to_mini,
  status, scale, rule
) VALUES
  (10003, '京东活动-示例3',  'https://img.alicdn.com/imgextra/i3/O1CN01W1W1W1W1W1W1W1W1W_!!6000000003000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 80, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10004, '京东活动-示例4',  'https://img.alicdn.com/imgextra/i2/O1CN01W1W1W1W1W1W1W1W1W_!!6000000004000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 70, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10005, '京东活动-示例5',  'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000005000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 60, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10006, '京东活动-示例6',  'https://img.alicdn.com/imgextra/i3/O1CN01W1W1W1W1W1W1W1W1W_!!6000000006000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 50, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10007, '京东活动-示例7',  'https://img.alicdn.com/imgextra/i2/O1CN01W1W1W1W1W1W1W1W1W_!!6000000007000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 40, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10008, '京东活动-示例8',  'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000008000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 30, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10009, '京东活动-示例9',  'https://img.alicdn.com/imgextra/i3/O1CN01W1W1W1W1W1W1W1W1W_!!6000000009000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 20, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10010, '京东活动-示例10', 'https://img.alicdn.com/imgextra/i2/O1CN01W1W1W1W1W1W1W1W1W_!!6000000010000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 10, '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10011, '京东活动-示例11', 'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000011000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 5,  '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}'),
  (10012, '京东活动-示例12', 'https://img.alicdn.com/imgextra/i3/O1CN01W1W1W1W1W1W1W1W1W_!!6000000012000-0-tps-1125-350.jpg', 'https://u.jd.com/EgtBMrL', 'jd', TRUE, TRUE, FALSE, FALSE, 1, 1,  '{"clickURL":"https://union-click.jd.com/jdc?e=1002294009&p=JF8BAOcJK1olXDYDZBoCUBVIMzZNXhpXVhgcFR0DFxcIWDoXSQVJQ1pSCQNDWBlSWyhcBS5SKmdeLj0NSkNhazR9ejBOOA59MV5YeztQBHFQRA5BFBlbEQIAODISBXFLZAYdPnh8UwwIcwlKezdRTysZUTYDZF1cCk0eAW8BHFslbQYCZBUzCXsfC2c4G10SWwAKUV1aCkkUBF8IE1wlXgUCVlheCUkLC24ME1sWbTYyV25dCUoUAGcNGlodbTYAZF1tViUWBGsMGA9BCWheDltfC01ABgEIGV4TWQIGUl9tCkoWAW04K2slbTY","shortURL":"https://u.jd.com/EgtBMrL","jdAppUrl":""}')
ON CONFLICT (act_id) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  banner = EXCLUDED.banner,
  path = EXCLUDED.path,
  jump_type = EXCLUDED.jump_type,
  support_banner = EXCLUDED.support_banner,
  support_app = EXCLUDED.support_app,
  support_mini = EXCLUDED.support_mini,
  app_to_mini = EXCLUDED.app_to_mini,
  status = EXCLUDED.status,
  scale = EXCLUDED.scale,
  rule = EXCLUDED.rule;

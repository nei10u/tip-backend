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
('限时秒杀', 'https://img.alicdn.com/imgextra/i2/O1CN01W1W1W1W1W1W1W1W1W_!!6000000001000-0-tps-500-250.jpg', 'https://miaosha.jd.com', 'jd', 1, 1),
('百亿补贴', 'https://img.alicdn.com/imgextra/i4/O1CN01W1W1W1W1W1W1W1W1W_!!6000000002000-0-tps-500-250.jpg', 'https://ju.taobao.com', 'taobao', 2, 1);

-- 初始化 Notice 数据
INSERT INTO home_notice (content, link_url, sort_order, status) VALUES
('双11超级红包已发放，点击领取！', 'https://s.click.taobao.com/redpacket', 1, 1),
('新人首单0元购，限时3天', 'https://s.click.taobao.com/newuser', 2, 1);

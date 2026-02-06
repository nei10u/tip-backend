-- Create table for Home Banners
CREATE TABLE IF NOT EXISTS home_banner (
  id BIGSERIAL PRIMARY KEY,
  title varchar(100) DEFAULT NULL,
  image_url text NOT NULL,
  link_url text DEFAULT NULL,
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Menus (Grid Icons)
CREATE TABLE IF NOT EXISTS home_menu (
  id BIGSERIAL PRIMARY KEY,
  label varchar(50) NOT NULL,
  icon_url text NOT NULL,
  link_url text DEFAULT NULL,
  type varchar(20) DEFAULT 'route',
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Activity Sections
CREATE TABLE IF NOT EXISTS home_section (
  id BIGSERIAL PRIMARY KEY,
  title varchar(100) DEFAULT NULL,
  image_url text NOT NULL,
  link_url text DEFAULT NULL,
  provider varchar(20) DEFAULT 'general',
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Notices (Marquee)
CREATE TABLE IF NOT EXISTS home_notice (
  id BIGSERIAL PRIMARY KEY,
  content text NOT NULL,
  link_url text DEFAULT NULL,
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    relation_id BIGINT,
    special_id BIGINT,
    tb_user_id VARCHAR(64),
    pdd_pid VARCHAR(64),
    jd_auth_id VARCHAR(128),
    mn_open_id VARCHAR(64),
    mp_open_id VARCHAR(64),
    union_id VARCHAR(64),
    alipay_uid VARCHAR(64),
    id_card_num VARCHAR(64),
    real_name VARCHAR(64),
    nickname VARCHAR(64),
    avatar_url VARCHAR(512),
    ali_pay_name VARCHAR(64),
    mp_status BOOLEAN DEFAULT FALSE,
    pdd_status BOOLEAN DEFAULT FALSE,
    jd_status BOOLEAN DEFAULT FALSE,
    user_discount DECIMAL(10, 4),
    total_actual_fee DECIMAL(10, 2),
    frozen_fee DECIMAL(10, 2),
    phone VARCHAR(20),
    email VARCHAR(64),
    token VARCHAR(128),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 兼容历史数据库：增量加字段（若表已存在）
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS ali_pay_name VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS mp_status BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS pdd_status BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS jd_auth_id VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS jd_status BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_discount DECIMAL(10, 4);
ALTER TABLE users ADD COLUMN IF NOT EXISTS total_actual_fee DECIMAL(10, 2);
ALTER TABLE users ADD COLUMN IF NOT EXISTS frozen_fee DECIMAL(10, 2);

-- 索引（放在增量加字段之后，避免历史库缺列导致建索引失败）
CREATE INDEX IF NOT EXISTS idx_users_relation_id ON users(relation_id);
CREATE INDEX IF NOT EXISTS idx_users_union_id ON users(union_id);
CREATE INDEX IF NOT EXISTS idx_users_token ON users(token);
CREATE INDEX IF NOT EXISTS idx_users_jd_auth_id ON users(jd_auth_id);

-- 模拟用户数据（用于本地调试）
INSERT INTO users (
    id, relation_id, special_id, tb_user_id, pdd_pid,
    mn_open_id, mp_open_id, union_id,
    alipay_uid, ali_pay_name,
    id_card_num, real_name,
    nickname, avatar_url,
    mp_status, pdd_status,
    user_discount, total_actual_fee, frozen_fee,
    phone, email, token, status,
    create_time, update_time
) VALUES (
    999, 999999, 888888, 'mock_tb_user_999', 'mock_pdd_pid_999',
    'mock_mn_open_999', 'mock_mp_open_999', 'mock_union_999',
    'mock_alipay_uid_999', '张三',
    '110101199001010000', '张三',
    '测试用户999', 'https://example.com/avatar-999.png',
    FALSE, FALSE,
    1.0000, 0.00, 0.00,
    '13900009999', 'u999@example.com', 'mock-token-999', 1,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 推进自增序列，避免后续插入撞到 999（若序列存在）
SELECT setval(
    pg_get_serial_sequence('users', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 0) FROM users), 999)
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_sn VARCHAR(64) NOT NULL UNIQUE,
    ds_order_sn VARCHAR(64),
    order_title VARCHAR(255),
    img VARCHAR(512),
    -- 平台用户ID（本站用户主键），长期推荐的强关联字段
    user_id BIGINT,
    sid VARCHAR(64),
    relation_id BIGINT,
    special_id BIGINT,
    adzone_id BIGINT,
    union_platform VARCHAR(32),
    type_no INTEGER,
    type_name VARCHAR(32),
    order_price DECIMAL(10, 2),
    pay_price DECIMAL(10, 2),
    share_rate DECIMAL(10, 2),
    share_fee DECIMAL(10, 2),
    -- 已入账金额（订单维度的“实际计入余额”），用于幂等对账：desired_credit - credited_fee => delta
    credited_fee DECIMAL(10, 2) DEFAULT 0.00,
    gross_share_fee DECIMAL(10, 2),
    base_deduction_rate DECIMAL(10, 4),
    base_deduction_fee DECIMAL(10, 2),
    platform_profit_rate DECIMAL(10, 4),
    platform_profit_fee DECIMAL(10, 2),
    user_discount DECIMAL(10, 4),
    order_discount DECIMAL(10, 4),
    order_status SMALLINT,
    status_content VARCHAR(32),
    -- 平台原始状态 / 退款 / 风控
    order_real_status INTEGER,
    refund_status INTEGER,
    order_lock INTEGER,
    punish_reason VARCHAR(255),
    pay_time TIMESTAMP,
    earn_time TIMESTAMP,
    modify_time TIMESTAMP,
    -- 可审计分期 key：建议按“应结算日”存 yyyyMMdd（例如 20251220）
    pay_month VARCHAR(8),
    -- 预估结算日期（yyyy-MM-dd），便于直接展示
    estimate_date VARCHAR(16),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 兼容历史数据库：增量加字段（若表已存在）
ALTER TABLE orders ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS union_platform VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS relation_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS special_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS adzone_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS gross_share_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS credited_fee DECIMAL(10, 2) DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS base_deduction_rate DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS base_deduction_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS platform_profit_rate DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS platform_profit_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS user_discount DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_discount DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_real_status INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_status INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_lock INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS punish_reason VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS earn_time TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS modify_time TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS pay_month VARCHAR(8);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimate_date VARCHAR(16);

-- 索引（放在增量加字段之后，避免历史库缺列导致建索引失败）
CREATE INDEX IF NOT EXISTS idx_orders_sid ON orders(sid);
CREATE INDEX IF NOT EXISTS idx_orders_relation_id ON orders(relation_id);
CREATE INDEX IF NOT EXISTS idx_orders_special_id ON orders(special_id);
CREATE INDEX IF NOT EXISTS idx_orders_adzone_id ON orders(adzone_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_create_time ON orders(create_time);

CREATE INDEX IF NOT EXISTS idx_orders_pay_month ON orders(pay_month);

-- 资金表
CREATE TABLE IF NOT EXISTS money (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    balance DECIMAL(10, 2) DEFAULT 0.00,
    frozen DECIMAL(10, 2) DEFAULT 0.00,
    total_income DECIMAL(10, 2) DEFAULT 0.00,
    total_withdraw DECIMAL(10, 2) DEFAULT 0.00,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 资金流水表（审计与幂等）
CREATE TABLE IF NOT EXISTS money_change (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_sn VARCHAR(64) NOT NULL,
    change_type SMALLINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    uuid VARCHAR(128) NOT NULL UNIQUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_money_change_user_id ON money_change(user_id);
CREATE INDEX IF NOT EXISTS idx_money_change_order_sn ON money_change(order_sn);

-- 淘宝退款明细/证据链（最小可审计：保存原始 JSON；后续可再结构化拆字段）
CREATE TABLE IF NOT EXISTS tb_order_refund (
    id BIGSERIAL PRIMARY KEY,
    trade_id VARCHAR(64) NOT NULL UNIQUE,
    order_sn VARCHAR(64),
    raw_json TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tb_order_refund_order_sn ON tb_order_refund(order_sn);

-- 转链/分享：推广信息表（用于生成可追踪的分享链接）
CREATE TABLE IF NOT EXISTS promotion_info (
    id BIGSERIAL PRIMARY KEY,
    goods_id VARCHAR(64) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    -- external_id 用于写入联盟转链参数（若联盟回传该字段，可用于反查归因/点击链路）
    external_id VARCHAR(64),
    promotion_url TEXT,
    user_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expire_time TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_promotion_info_user_id ON promotion_info(user_id);
CREATE INDEX IF NOT EXISTS idx_promotion_info_goods_platform ON promotion_info(goods_id, platform);
CREATE UNIQUE INDEX IF NOT EXISTS uq_promotion_info_external_id ON promotion_info(external_id);

-- 转链/分享：点击埋点表（记录每次跳转）
CREATE TABLE IF NOT EXISTS promotion_click (
    id BIGSERIAL PRIMARY KEY,
    info_id BIGINT NOT NULL,
    user_id BIGINT,
    click_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_promotion_click_info_id ON promotion_click(info_id);
CREATE INDEX IF NOT EXISTS idx_promotion_click_user_id ON promotion_click(user_id);

-- 淘宝商品表
CREATE TABLE IF NOT EXISTS dtk_goods (
    id BIGSERIAL PRIMARY KEY,
    goods_id VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255),
    dtitle VARCHAR(255),
    description TEXT,
    main_pic VARCHAR(512),
    marketing_main_pic VARCHAR(512),
    price DECIMAL(10, 2),
    original_price DECIMAL(10, 2),
    coupon_price DECIMAL(10, 2),
    coupon_link VARCHAR(512),
    coupon_start_time TIMESTAMP,
    coupon_end_time TIMESTAMP,
    commission_rate DECIMAL(10, 2),
    sales_volume INTEGER,
    shop_type INTEGER,
    shop_name VARCHAR(128),
    shop_level INTEGER,
    brand_name VARCHAR(128),
    platform VARCHAR(32),
    activity_type INTEGER,
    activity_start_time TIMESTAMP,
    activity_end_time TIMESTAMP,
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
-- PostgreSQL 的 COMMENT 语法
COMMENT ON COLUMN dtk_goods.status IS 'Status (1: Normal, 0: Stale)';
CREATE INDEX IF NOT EXISTS idx_dtk_goods_update_time ON dtk_goods(update_time);

-- ============================================================
-- 活动系统（从 BOOT-INF 反推）：ActivityBean / ActivityCategoryBean
-- 以及相关的响应/平台配置类（可选落库）
--
-- 说明：
-- - ActivityBean 对应 @TableName("ds_config_activity")
-- - ActivityCategoryBean 对应 @TableName("ds_activity_category")
-- - ActivityCategoryParentHashMapResponse / ActivityCategoryResponse 是运行期拼装结构：
--   这里提供“可选缓存表”用于预计算/固化结构，便于运营后台管理与调试
-- - ActivityPlatformBean 为简单的平台枚举配置（可选）
-- ============================================================

-- 活动表：ds_config_activity（ActivityBean）
CREATE TABLE IF NOT EXISTS ds_config_activity (
    id BIGSERIAL PRIMARY KEY,
    -- 业务活动ID：代码中以 act_id 作为 Cache.ACTIVITY_MAP 的 key
    act_id INTEGER NOT NULL UNIQUE,
    icon VARCHAR(512),
    des TEXT,
    commission_rate VARCHAR(64),
    -- 说明：后端 updateActivityList() 用 QueryWrapper.eq("status", 1) 过滤，因此这里用整数更贴近真实行为
    status INTEGER DEFAULT 1,
    display_name VARCHAR(128),
    package_name VARCHAR(256),
    tips VARCHAR(255),
    banner VARCHAR(512),
    top_tips VARCHAR(255),
    path VARCHAR(1024),
    jump_type VARCHAR(32),
    support_banner BOOLEAN DEFAULT FALSE,
    support_app BOOLEAN DEFAULT TRUE,
    support_mini BOOLEAN DEFAULT FALSE,
    app_to_mini BOOLEAN DEFAULT FALSE,
    mini_g_id VARCHAR(128),
    scale INTEGER DEFAULT 0,
    rule TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ds_config_activity IS '活动配置表（对应 ActivityBean / Cache.ACTIVITY_MAP）';
COMMENT ON COLUMN ds_config_activity.act_id IS '业务活动ID（接口/缓存使用，非自增主键）';
COMMENT ON COLUMN ds_config_activity.path IS '活动跳转目标（H5链接/物料链接/业务path等）';
COMMENT ON COLUMN ds_config_activity.jump_type IS '跳转类型（app/mini/h5/other，按前端约定）';
COMMENT ON COLUMN ds_config_activity.mini_g_id IS '小程序原始ID（gh_xxx），用于跳转到小程序';
COMMENT ON COLUMN ds_config_activity.status IS '状态：1=启用；0=停用（后端按 status=1 取数）';

CREATE INDEX IF NOT EXISTS idx_ds_config_activity_status ON ds_config_activity(status);
CREATE INDEX IF NOT EXISTS idx_ds_config_activity_support ON ds_config_activity(support_app, support_mini, support_banner);

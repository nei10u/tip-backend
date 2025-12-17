-- Create table for Home Banners
CREATE TABLE IF NOT EXISTS home_banner (
  id BIGSERIAL PRIMARY KEY,
  title varchar(100) DEFAULT NULL,
  image_url varchar(500) NOT NULL,
  link_url varchar(500) DEFAULT NULL,
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Menus (Grid Icons)
CREATE TABLE IF NOT EXISTS home_menu (
  id BIGSERIAL PRIMARY KEY,
  label varchar(50) NOT NULL,
  icon_url varchar(500) NOT NULL,
  link_url varchar(500) DEFAULT NULL,
  type varchar(20) DEFAULT 'route',
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Activity Sections
CREATE TABLE IF NOT EXISTS home_section (
  id BIGSERIAL PRIMARY KEY,
  title varchar(100) DEFAULT NULL,
  image_url varchar(500) NOT NULL,
  link_url varchar(500) DEFAULT NULL,
  provider varchar(20) DEFAULT 'general',
  sort_order int DEFAULT 0,
  status int DEFAULT 1,
  created_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- Create table for Home Notices (Marquee)
CREATE TABLE IF NOT EXISTS home_notice (
  id BIGSERIAL PRIMARY KEY,
  content varchar(255) NOT NULL,
  link_url varchar(500) DEFAULT NULL,
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
    mn_open_id VARCHAR(64),
    mp_open_id VARCHAR(64),
    union_id VARCHAR(64),
    alipay_uid VARCHAR(64),
    id_card_num VARCHAR(64),
    real_name VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(64),
    token VARCHAR(128),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_relation_id ON users(relation_id);
CREATE INDEX IF NOT EXISTS idx_users_union_id ON users(union_id);
CREATE INDEX IF NOT EXISTS idx_users_token ON users(token);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_sn VARCHAR(64) NOT NULL UNIQUE,
    ds_order_sn VARCHAR(64),
    order_title VARCHAR(255),
    img VARCHAR(512),
    sid VARCHAR(64),
    union_platform VARCHAR(32),
    type_no INTEGER,
    type_name VARCHAR(32),
    order_price DECIMAL(10, 2),
    pay_price DECIMAL(10, 2),
    share_rate DECIMAL(10, 2),
    share_fee DECIMAL(10, 2),
    gross_share_fee DECIMAL(10, 2),
    base_deduction_rate DECIMAL(10, 4),
    base_deduction_fee DECIMAL(10, 2),
    platform_profit_rate DECIMAL(10, 4),
    platform_profit_fee DECIMAL(10, 2),
    user_discount DECIMAL(10, 4),
    order_discount DECIMAL(10, 4),
    order_status SMALLINT,
    status_content VARCHAR(32),
    pay_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_sid ON orders(sid);
CREATE INDEX IF NOT EXISTS idx_orders_create_time ON orders(create_time);

-- 兼容历史数据库：增量加字段（若表已存在）
ALTER TABLE orders ADD COLUMN IF NOT EXISTS union_platform VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS gross_share_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS base_deduction_rate DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS base_deduction_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS platform_profit_rate DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS platform_profit_fee DECIMAL(10, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS user_discount DECIMAL(10, 4);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_discount DECIMAL(10, 4);

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

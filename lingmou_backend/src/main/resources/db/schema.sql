-- ============================================
-- ICBC · 灵枢 数据库建表脚本
-- 数据库名: lingmou
-- 创建时间: 2026-09-08
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS lingmou DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lingmou;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(50)  UNIQUE NOT NULL COMMENT '用户名',
    password        VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(50) COMMENT '真实姓名',
    id_card         VARCHAR(18) COMMENT '身份证号',
    phone           VARCHAR(20) COMMENT '手机号',
    avatar          VARCHAR(255) COMMENT '头像URL',
    role            VARCHAR(20) DEFAULT 'CUSTOMER' COMMENT '角色: CUSTOMER/AUDITOR/RISK/ADMIN',
    customer_level  VARCHAR(20) DEFAULT 'NORMAL' COMMENT '客户级别: NORMAL/SILVER/GOLD',
    credit_score    INT DEFAULT 90 COMMENT '信用分（初始90，正向行为可升至100）',
    elderly_mode    TINYINT DEFAULT 0 COMMENT '老年模式: 0-关闭 1-开启',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 网点表（字段对齐前端 branch.ts interface Branch）
-- ============================================
DROP TABLE IF EXISTS branches;
CREATE TABLE branches (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '网点ID(内部主键1-6)',
    branch_code     VARCHAR(10) UNIQUE NOT NULL COMMENT '网点编码：b1~b6（前端id）',
    name            VARCHAR(100) NOT NULL COMMENT '网点名称',
    address         VARCHAR(200) COMMENT '网点地址',
    phone           VARCHAR(20) COMMENT '联系电话',
    hours           VARCHAR(50) DEFAULT '09:00 - 17:00' COMMENT '营业时间',
    distance        INT DEFAULT 0 COMMENT '距离(米)',
    services        TEXT COMMENT '业务标签数组JSON，如["大额现金","外汇"]',
    wait_time       INT DEFAULT 0 COMMENT '等待时间(分钟)',
    flow_count      INT DEFAULT 0 COMMENT '在店人数',
    reserve_count   INT DEFAULT 0 COMMENT '线上预约人数',
    window_info     VARCHAR(20) COMMENT '窗口信息如8/10',
    trend           TEXT COMMENT '人流趋势数组JSON如[18,22,28]',
    busy_level      VARCHAR(10) DEFAULT 'free' COMMENT '繁忙程度: free/moderate/busy',
    cover_image     VARCHAR(255) COMMENT '网点封面图URL',
    icon            VARCHAR(100) COMMENT '图标class如fa-solid fa-landmark',
    icon_bg         VARCHAR(50) COMMENT '图标背景class如bg-blue-50',
    icon_color      VARCHAR(50) COMMENT '图标颜色class如text-blue-600',
    favorite        TINYINT DEFAULT 0 COMMENT '收藏: 0-否 1-是',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_name (name),
    INDEX idx_branch_code (branch_code),
    INDEX idx_busy_level (busy_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网点表';

-- ============================================
-- 3. 预约表
-- ============================================
DROP TABLE IF EXISTS appointments;
CREATE TABLE appointments (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    branch_id           BIGINT NOT NULL COMMENT '网点ID',
    business_type       VARCHAR(50) COMMENT '业务类型',
    appointment_date    DATE NOT NULL COMMENT '预约日期',
    time_slot           VARCHAR(20) NOT NULL COMMENT '时段，如 09:00-09:30',
    queue_number        VARCHAR(20) COMMENT '排队号',
    status              VARCHAR(20) DEFAULT 'VIRTUAL' COMMENT '状态: VIRTUAL/ACTIVE/CALLED/PROCESSING/COMPLETED/EXPIRED/CANCELED',
    voucher_num         VARCHAR(50) UNIQUE COMMENT '凭证号',
    progress_step       INT DEFAULT 0 COMMENT '进度步骤: 0-取号 1-排队 2-叫号 3-办理 4-完成',
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_date_slot_user (appointment_date, time_slot, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_branch_id (branch_id),
    INDEX idx_status (status),
    INDEX idx_appointment_date (appointment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约表';

-- ============================================
-- 4. 凭证表
-- ============================================
DROP TABLE IF EXISTS vouchers;
CREATE TABLE vouchers (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '凭证ID',
    voucher_num     VARCHAR(50) UNIQUE NOT NULL COMMENT '凭证号',
    appointment_id  BIGINT COMMENT '关联预约ID',
    qrcode_content  TEXT COMMENT '二维码内容',
    sn              VARCHAR(50) COMMENT '流水号SN',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_voucher_num (voucher_num),
    INDEX idx_appointment_id (appointment_id),
    INDEX idx_sn (sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='凭证表';

-- ============================================
-- 5. 预填单表
-- ============================================
DROP TABLE IF EXISTS pre_forms;
CREATE TABLE pre_forms (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预填单ID',
    user_id         BIGINT COMMENT '用户ID',
    business_type   VARCHAR(50) COMMENT '业务类型',
    raw_text        TEXT COMMENT '原始文本',
    parsed_json     TEXT COMMENT '解析后的JSON',
    image_urls      TEXT COMMENT '上传的材料图片URL列表（JSON数组）',
    signature_url   VARCHAR(255) COMMENT '签名图片URL',
    status          VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/SUBMITTED/USED',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_business_type (business_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预填单表';

-- ============================================
-- 6. 审计日志表（区块链式）
-- ============================================
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id     BIGINT COMMENT '操作人ID',
    operator_name  VARCHAR(50) COMMENT '操作人姓名',
    action          VARCHAR(100) NOT NULL COMMENT '操作类型',
    content         TEXT COMMENT '操作内容',
    prev_hash       VARCHAR(64) COMMENT '前一条记录的hash',
    hash            VARCHAR(64) NOT NULL COMMENT '当前记录的hash',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_operator_id (operator_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- ============================================
-- 7. 信用分变更记录表
-- ============================================
DROP TABLE IF EXISTS credit_records;
CREATE TABLE credit_records (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    change_type     VARCHAR(20) NOT NULL COMMENT '变更类型: ADD/DEDUCT',
    amount          INT NOT NULL COMMENT '变更分数（正负）',
    reason          VARCHAR(200) COMMENT '变更原因',
    operator_id     BIGINT COMMENT '操作人ID（系统为0）',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_change_type (change_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信用分变更记录表';

-- ============================================
-- 初始化网点种子数据（6个北京网点，逐字对齐前端 branch.ts）
-- ============================================
INSERT INTO branches (branch_code, name, busy_level, distance, services, wait_time, flow_count, reserve_count, window_info, trend, address, phone, hours, icon, icon_bg, icon_color, favorite) VALUES
('b1', '北京分行营业部',         'busy',     850,  '["大额现金","外汇","无障碍"]', 25, 42, 18, '8/10', '[18,22,28,20,25,25]', '北京市西城区复兴门内大街55号',              '010-66695588', '09:00 - 17:00', 'fa-solid fa-landmark',   'bg-blue-50',    'text-blue-600',    0),
('b2', '长安街智慧示范支行',     'moderate', 1400, '["自助发卡","VTM"]',           8, 19,  7, '5/6',  '[12,10,6,9,7,8]',    '北京市东城区东长安街1号',                  '010-65129588', '09:00 - 17:00', 'fa-solid fa-robot',      'bg-cyan-50',    'text-cyan-600',    0),
('b3', '金融街私人银行旗舰支行', 'free',     2100, '["VIP"]',                       3,  8,  3, '6/6',  '[5,4,3,2,3,3]',      '北京市西城区金融大街15号',                  '010-66299588', '09:00 - 17:30', 'fa-solid fa-crown',      'bg-emerald-50', 'text-emerald-600', 0),
('b4', '中关村科技创新特色支行', 'moderate', 3800, '["对公"]',                      12, 25, 12, '6/8',  '[8,15,10,14,11,12]', '北京市海淀区中关村大街22号',                '010-62599588', '09:00 - 17:00', 'fa-solid fa-microchip',  'bg-purple-50', 'text-purple-600', 0),
('b5', '望京SOHO社区支行',       'free',     3200, '["自助发卡","无障碍"]',          5, 10,  4, '4/4',  '[8,6,4,7,5,5]',      '北京市朝阳区望京街10号望京SOHO塔1座',       '010-59799588', '09:00 - 17:00', 'fa-solid fa-shop',       'bg-emerald-50', 'text-emerald-600', 0),
('b6', '国贸CBD中心支行',        'busy',     1800, '["外汇","VIP","对公"]',          20, 38, 15, '7/8',  '[15,18,22,17,20,20]', '北京市朝阳区建国门外大街1号国贸大厦',         '010-65059588', '09:00 - 17:00', 'fa-solid fa-city',       'bg-blue-50',    'text-blue-600',    0);

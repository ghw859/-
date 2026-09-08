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
    role            VARCHAR(20) DEFAULT 'CUSTOMER' COMMENT '角色: CUSTOMER/AUDITOR/RISK/ADMIN',
    customer_level  VARCHAR(20) DEFAULT 'NORMAL' COMMENT '客户级别: NORMAL/SILVER/GOLD',
    credit_score    INT DEFAULT 100 COMMENT '信用分',
    elderly_mode    TINYINT DEFAULT 0 COMMENT '老年模式: 0-关闭 1-开启',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 网点表
-- ============================================
DROP TABLE IF EXISTS branches;
CREATE TABLE branches (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '网点ID',
    name            VARCHAR(100) NOT NULL COMMENT '网点名称',
    address         VARCHAR(200) COMMENT '网点地址',
    business_hours  VARCHAR(50) DEFAULT '09:00-17:00' COMMENT '营业时间',
    current_queue   INT DEFAULT 0 COMMENT '当前排队人数',
    busy_level      VARCHAR(10) DEFAULT 'IDLE' COMMENT '繁忙程度: IDLE/MODERATE/BUSY',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_name (name),
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
-- 初始化网点种子数据
-- ============================================
INSERT INTO branches (name, address, business_hours, current_queue, busy_level) VALUES
('工商银行北京西单支行', '北京市西城区西单北大街120号', '09:00-17:00', 0, 'IDLE'),
('工商银行上海浦东支行', '上海市浦东新区世纪大道100号', '09:00-17:00', 0, 'IDLE'),
('工商银行深圳南山支行', '深圳市南山区科技园南区高新南一道', '09:00-17:00', 0, 'IDLE'),
('工商银行杭州西湖支行', '杭州市西湖区曙光路128号', '09:00-17:00', 0, 'IDLE');

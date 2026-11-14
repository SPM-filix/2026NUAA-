-- 校园垃圾回收积分系统数据库初始化脚本
-- 运行环境：MySQL 8.0

CREATE DATABASE IF NOT EXISTS recycle CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE recycle;

-- 1. 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_code` CHAR(6) NOT NULL UNIQUE COMMENT '6位用户编号',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `password_hash` VARCHAR(100) NOT NULL,
    `nickname` VARCHAR(20) NOT NULL,
    `avatar_url` VARCHAR(255),
    `address` VARCHAR(100),
    `points_balance` INT NOT NULL DEFAULT 0 CHECK (`points_balance` >= 0),
    `token_version` INT NOT NULL DEFAULT 0,
    `must_change_password` TINYINT(1) NOT NULL DEFAULT 0,
    `status` VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `disabled_reason` VARCHAR(200),
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 回收员表
CREATE TABLE IF NOT EXISTS `recycler` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(20) NOT NULL,
    `phone` VARCHAR(11) NOT NULL UNIQUE,
    `password_hash` VARCHAR(100) NOT NULL,
    `employee_no` VARCHAR(20) NOT NULL,
    `site_id` BIGINT,
    `status` VARCHAR(10) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/DISABLED',
    `token_version` INT NOT NULL DEFAULT 0,
    `must_change_password` TINYINT(1) NOT NULL DEFAULT 0,
    `apply_reason` VARCHAR(200),
    `reject_reason` VARCHAR(200),
    `approved_by` BIGINT,
    `approved_at` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_phone` (`phone`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收员表';

-- 3. 管理员表
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(30) NOT NULL UNIQUE,
    `password_hash` VARCHAR(100) NOT NULL,
    `status` VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    `token_version` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 4. 回收站点表
CREATE TABLE IF NOT EXISTS `site` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(30) NOT NULL,
    `address` VARCHAR(100) NOT NULL,
    `open_time` VARCHAR(50),
    `manager` VARCHAR(20),
    `phone` VARCHAR(20),
    `status` VARCHAR(10) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='回收站点表';

-- 5. 垃圾类别表
CREATE TABLE IF NOT EXISTS `recycle_category` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(20) NOT NULL,
    `code` VARCHAR(20) NOT NULL UNIQUE,
    `unit` VARCHAR(10) NOT NULL DEFAULT 'kg',
    `icon_url` VARCHAR(255),
    `sort` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(10) NOT NULL DEFAULT 'ENABLED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_code` (`code`),
    INDEX `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='垃圾类别表';

-- 6. 积分规则表
CREATE TABLE IF NOT EXISTS `points_rule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` BIGINT NOT NULL,
    `points_per_kg` INT NOT NULL,
    `effective_from` DATETIME NOT NULL,
    `created_by` BIGINT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_category_effective` (`category_id`, `effective_from` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分规则历史表';

-- 7. 投递流水表
CREATE TABLE IF NOT EXISTS `delivery_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `record_no` VARCHAR(24) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `recycler_id` BIGINT NOT NULL,
    `site_id` BIGINT,
    `category_id` BIGINT NOT NULL,
    `rule_id` BIGINT,
    `weight` DECIMAL(6,2) NOT NULL CHECK (`weight` > 0),
    `points` INT NOT NULL,
    `status` VARCHAR(10) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/REVOKED',
    `remark` VARCHAR(100),
    `revoke_reason` VARCHAR(200),
    `revoked_by_type` VARCHAR(10),
    `revoked_by_id` BIGINT,
    `revoked_at` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_created` (`user_id`, `created_at` DESC),
    INDEX `idx_recycler_created` (`recycler_id`, `created_at` DESC),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递流水表';

-- 8. 积分明细表
CREATE TABLE IF NOT EXISTS `points_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(10) NOT NULL COMMENT 'INCOME/EXPENSE/REVOKE/ADJUST/REFUND',
    `change` INT NOT NULL CHECK (`change` != 0),
    `balance_after` INT NOT NULL,
    `biz_type` VARCHAR(20) NOT NULL,
    `biz_id` BIGINT,
    `biz_no` VARCHAR(24),
    `operator_type` VARCHAR(10),
    `operator_id` BIGINT,
    `remark` VARCHAR(200),
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_created` (`user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分明细表';

-- 9. 商品表
CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    `description` VARCHAR(500),
    `cover_url` VARCHAR(255),
    `points_price` INT NOT NULL CHECK (`points_price` >= 1),
    `stock` INT NOT NULL DEFAULT 0 CHECK (`stock` >= 0),
    `sold_count` INT NOT NULL DEFAULT 0,
    `spec` VARCHAR(100),
    `exchange_notice` VARCHAR(200),
    `status` VARCHAR(10) NOT NULL DEFAULT 'ON' COMMENT 'ON/OFF',
    `version` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 10. 兑换订单表
CREATE TABLE IF NOT EXISTS `exchange_order` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_no` VARCHAR(24) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL CHECK (`quantity` >= 1),
    `points_cost` INT NOT NULL,
    `status` VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/COMPLETED/CANCELLED',
    `redeem_code` CHAR(8) NOT NULL UNIQUE,
    `redeem_expire_at` DATETIME NOT NULL,
    `verified_by` BIGINT,
    `verified_site_id` BIGINT,
    `verified_at` DATETIME,
    `cancel_reason` VARCHAR(200),
    `cancelled_by` BIGINT,
    `idempotency_key` VARCHAR(64),
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_idempotency` (`user_id`, `idempotency_key`),
    INDEX `idx_user_created` (`user_id`, `created_at` DESC),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='兑换订单表';

-- 11. 公告表
CREATE TABLE IF NOT EXISTS `announcement` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(50) NOT NULL,
    `type` VARCHAR(12) NOT NULL COMMENT 'NOTICE/KNOWLEDGE',
    `content` TEXT NOT NULL,
    `cover_url` VARCHAR(255),
    `status` VARCHAR(12) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/OFFLINE',
    `published_at` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_status_published` (`status`, `published_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 12. 申诉表
CREATE TABLE IF NOT EXISTS `appeal` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `target_type` VARCHAR(10) NOT NULL COMMENT 'DELIVERY/EXCHANGE',
    `target_id` BIGINT NOT NULL,
    `description` VARCHAR(500) NOT NULL,
    `status` VARCHAR(12) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED',
    `conclusion` VARCHAR(500),
    `handled_by` BIGINT,
    `handled_at` DATETIME,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_status_created` (`status`, `created_at` DESC),
    INDEX `idx_user_created` (`user_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';

-- 13. 上传文件表
CREATE TABLE IF NOT EXISTS `upload_file` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `url` VARCHAR(255) NOT NULL,
    `storage_path` VARCHAR(255) NOT NULL,
    `size` INT NOT NULL,
    `mime` VARCHAR(50) NOT NULL,
    `scene` VARCHAR(20) NOT NULL COMMENT 'PRODUCT/AVATAR/ANNOUNCEMENT',
    `uploaded_by_type` VARCHAR(10) NOT NULL,
    `uploaded_by_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_uploader` (`uploaded_by_type`, `uploaded_by_id`, `created_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传文件表';

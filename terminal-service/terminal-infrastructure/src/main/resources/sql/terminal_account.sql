-- ============================================
-- 终端设备账号表
-- 用于存储LED设备的认证信息
-- ============================================

DROP TABLE IF EXISTS `terminal_account`;

CREATE TABLE `terminal_account` (
  `id` BIGINT NOT NULL COMMENT '主键ID',
  `device_id` VARCHAR(50) NOT NULL COMMENT '设备ID，全局唯一',
  `device_name` VARCHAR(100) NOT NULL COMMENT '设备名称',
  `device_type` VARCHAR(20) NOT NULL DEFAULT 'LED_SCREEN' COMMENT '设备类型：LED_SCREEN, LED_WALL, LCD_SCREEN',
  `password` VARCHAR(255) NOT NULL COMMENT '设备密码，BCrypt加密',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '设备状态：ACTIVE-活跃, INACTIVE-非活跃, DISABLED-禁用',
  `organization_id` VARCHAR(50) NOT NULL COMMENT '组织ID',
  `organization_name` VARCHAR(100) NOT NULL COMMENT '组织名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '设备描述',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `failed_attempts` INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
  `locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否锁定：0-未锁定, 1-已锁定',
  `locked_time` DATETIME DEFAULT NULL COMMENT '锁定时间',
  `locked_until` DATETIME DEFAULT NULL COMMENT '锁定过期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT 'system' COMMENT '创建者',
  `update_by` VARCHAR(50) DEFAULT 'system' COMMENT '更新者',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除, 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  KEY `idx_organization_id` (`organization_id`),
  KEY `idx_status` (`status`),
  KEY `idx_locked` (`locked`),
  KEY `idx_locked_until` (`locked_until`),
  KEY `idx_last_login_time` (`last_login_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='终端设备账号表';

-- ============================================
-- 初始化测试数据
-- ============================================

-- 插入测试设备账号
-- 密码: 123456 (BCrypt加密后的值)
INSERT INTO `terminal_account` (`id`, `device_id`, `device_name`, `device_type`, `password`, `status`, `organization_id`, `organization_name`, `description`, `create_by`, `update_by`) VALUES
(1, 'LED001', '大厅LED显示屏', 'LED_SCREEN', '$2a$10$N.zmdr9k7uOCQb0bfsb5.eFe.QbkIRPYBfnPY.gmlbr5kpj6T/VG6', 'ACTIVE', 'ORG001', '总部大厦', '位于大厅中央的主显示屏', 'admin', 'admin'),
(2, 'LED002', '会议室LED屏幕', 'LED_SCREEN', '$2a$10$N.zmdr9k7uOCQb0bfsb5.eFe.QbkIRPYBfnPY.gmlbr5kpj6T/VG6', 'ACTIVE', 'ORG001', '总部大厦', '会议室A的LED显示屏', 'admin', 'admin'),
(3, 'LED003', '户外LED广告屏', 'LED_WALL', '$2a$10$N.zmdr9k7uOCQb0bfsb5.eFe.QbkIRPYBfnPY.gmlbr5kpj6T/VG6', 'ACTIVE', 'ORG002', '分公司大楼', '户外大型LED广告墙', 'admin', 'admin'),
(4, 'LCD001', '前台LCD显示屏', 'LCD_SCREEN', '$2a$10$N.zmdr9k7uOCQb0bfsb5.eFe.QbkIRPYBfnPY.gmlbr5kpj6T/VG6', 'ACTIVE', 'ORG001', '总部大厦', '前台信息显示屏', 'admin', 'admin'),
(5, 'LED004', '待激活设备', 'LED_SCREEN', '$2a$10$N.zmdr9k7uOCQb0bfsb5.eFe.QbkIRPYBfnPY.gmlbr5kpj6T/VG6', 'INACTIVE', 'ORG001', '总部大厦', '待激活的LED设备', 'admin', 'admin');

-- ============================================
-- 创建索引优化查询性能
-- ============================================

-- 复合索引：组织ID + 状态 + 最后登录时间 (用于统计在线设备)
CREATE INDEX `idx_org_status_login` ON `terminal_account` (`organization_id`, `status`, `last_login_time`);

-- 复合索引：设备ID + 删除标记 (用于唯一性约束，支持软删除)
CREATE INDEX `idx_device_deleted` ON `terminal_account` (`device_id`, `deleted`);

-- 复合索引：锁定状态 + 锁定过期时间 (用于定时解锁任务)
CREATE INDEX `idx_locked_until` ON `terminal_account` (`locked`, `locked_until`);

-- ============================================
-- 创建视图：活跃设备统计
-- ============================================

CREATE VIEW `v_active_devices` AS
SELECT 
    `organization_id`,
    `organization_name`,
    COUNT(*) AS `total_devices`,
    SUM(CASE WHEN `status` = 'ACTIVE' THEN 1 ELSE 0 END) AS `active_devices`,
    SUM(CASE WHEN `status` = 'ACTIVE' AND `last_login_time` >= DATE_SUB(NOW(), INTERVAL 5 MINUTE) THEN 1 ELSE 0 END) AS `online_devices`,
    SUM(CASE WHEN `locked` = 1 THEN 1 ELSE 0 END) AS `locked_devices`
FROM `terminal_account`
WHERE `deleted` = 0
GROUP BY `organization_id`, `organization_name`;

-- ============================================
-- 创建存储过程：批量解锁过期账号
-- ============================================

DELIMITER $$

CREATE PROCEDURE `sp_unlock_expired_accounts`()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_device_id VARCHAR(50);
    DECLARE unlock_cursor CURSOR FOR 
        SELECT `device_id` 
        FROM `terminal_account` 
        WHERE `locked` = 1 AND `locked_until` <= NOW() AND `deleted` = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- 开启事务
    START TRANSACTION;
    
    -- 打开游标
    OPEN unlock_cursor;
    
    unlock_loop: LOOP
        FETCH unlock_cursor INTO v_device_id;
        IF done THEN
            LEAVE unlock_loop;
        END IF;
        
        -- 解锁账号
        UPDATE `terminal_account` 
        SET `locked` = 0, 
            `failed_attempts` = 0, 
            `locked_time` = NULL, 
            `locked_until` = NULL, 
            `update_time` = NOW(),
            `update_by` = 'system'
        WHERE `device_id` = v_device_id AND `deleted` = 0;
    END LOOP;
    
    -- 关闭游标
    CLOSE unlock_cursor;
    
    -- 提交事务
    COMMIT;
    
    -- 返回解锁的账号数量
    SELECT ROW_COUNT() AS unlocked_count;
END$$

DELIMITER ;

-- ============================================
-- 创建事件：自动解锁过期账号
-- ============================================

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;

-- 创建每5分钟执行一次的解锁事件
CREATE EVENT IF NOT EXISTS `evt_unlock_expired_accounts`
ON SCHEDULE EVERY 5 MINUTE
STARTS CURRENT_TIMESTAMP
DO
  CALL sp_unlock_expired_accounts();

-- ============================================
-- 性能优化建议
-- ============================================

/*
1. 定期清理过期数据：
   - 软删除的设备记录可以定期物理删除
   - 失败登录次数可以定期重置

2. 监控关键指标：
   - 锁定账号数量
   - 认证失败率
   - 平均认证响应时间

3. 缓存策略：
   - 活跃设备认证信息缓存30分钟
   - 失败认证缓存5分钟防止暴力破解
   - 锁定状态缓存15分钟

4. 分表策略（大规模部署）：
   - 可按组织ID或设备ID范围分表
   - 历史登录记录可单独存储
*/
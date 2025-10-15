-- 权限表达式系统升级
-- 添加binding_type字段支持INCLUDE/EXCLUDE权限类型

-- 1. 添加binding_type字段
ALTER TABLE user_group_terminal_group_rel 
ADD COLUMN binding_type VARCHAR(20) NOT NULL DEFAULT 'INCLUDE' 
COMMENT '绑定类型：INCLUDE-包含，EXCLUDE-排除' 
AFTER include_sub;

-- 2. 创建复合索引，优化查询性能
CREATE INDEX idx_ugid_binding_type ON user_group_terminal_group_rel(ugid, binding_type);
CREATE INDEX idx_tgid_binding_type ON user_group_terminal_group_rel(tgid, binding_type);

-- 3. 添加约束确保binding_type值有效
ALTER TABLE user_group_terminal_group_rel 
ADD CONSTRAINT chk_binding_type 
CHECK (binding_type IN ('INCLUDE', 'EXCLUDE'));

-- 4. 更新表注释
ALTER TABLE user_group_terminal_group_rel 
COMMENT = '用户组-终端组绑定关系表，支持INCLUDE/EXCLUDE权限表达式';

-- 5. 为现有数据设置默认值（如果有数据的话）
UPDATE user_group_terminal_group_rel 
SET binding_type = 'INCLUDE' 
WHERE binding_type IS NULL OR binding_type = '';

-- 验证更新
SELECT COUNT(*) as total_records,
       binding_type,
       COUNT(*) as count_by_type
FROM user_group_terminal_group_rel 
GROUP BY binding_type;
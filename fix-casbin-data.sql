-- 修复Casbin RBAC规则数据
-- 问题：代码中addPolicy缺少eft参数，导致policy size不匹配

-- 1. 备份当前数据
CREATE TABLE rbac_casbin_rules_backup AS SELECT * FROM rbac_casbin_rules;

-- 2. 更新现有策略数据，为NULL的v4字段设置为'allow'
UPDATE rbac_casbin_rules 
SET v4 = 'allow' 
WHERE ptype = 'p' AND (v4 IS NULL OR v4 = '');

-- 3. 验证Casbin在MySQL中对NULL值的处理
-- MySQL中NULL在Casbin中会被视为空字符串，导致匹配失败
-- 因此我们需要确保所有策略都有明确的eft值

-- 4. 查看修复后的数据
SELECT 'g规则数量' as type, COUNT(*) as count FROM rbac_casbin_rules WHERE ptype = 'g'
UNION ALL
SELECT 'p规则数量(allow)' as type, COUNT(*) as count FROM rbac_casbin_rules WHERE ptype = 'p' AND v4 = 'allow'
UNION ALL
SELECT 'p规则数量(deny)' as type, COUNT(*) as count FROM rbac_casbin_rules WHERE ptype = 'p' AND v4 = 'deny'
UNION ALL
SELECT 'p规则数量(NULL)' as type, COUNT(*) as count FROM rbac_casbin_rules WHERE ptype = 'p' AND (v4 IS NULL OR v4 = '');

-- 5. 显示修复后的数据结构
SELECT id, ptype, v0, v1, v2, v3, v4, v5 FROM rbac_casbin_rules ORDER BY ptype, id;

-- 6. 示例：为特定用户添加拒绝权限
-- INSERT INTO rbac_casbin_rules (ptype, v0, v1, v2, v3, v4) VALUES
-- ('p', '用户ID', '组织ID', '/core/api/sensitive/action', 'POST', 'deny');

-- 7. 说明：
-- - v4字段说明：
--   * 'allow': 允许访问（默认）
--   * 'deny': 拒绝访问（优先级更高）
--   * NULL或空字符串: 在Casbin中会导致匹配失败
-- 
-- - Casbin的policy_effect配置：
--   * e = some(where (p.eft == allow)) && !some(where (p.eft == deny))
--   * 意思是：存在allow且不存在deny时才允许访问
--   * deny权限具有更高优先级，会覆盖allow权限
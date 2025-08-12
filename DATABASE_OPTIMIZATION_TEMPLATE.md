# 数据库优化建议 - 模板管理功能

## 概述

本文档针对新增的模板管理功能提供数据库性能优化建议，包括索引设计、查询优化和架构改进方案。

## 索引优化建议

### 1. 模板查询索引

#### 主查询索引
```sql
-- 模板列表查询的复合索引（支持继承查询）
CREATE INDEX idx_program_template_query ON t_program(
    org_id, 
    program_status, 
    user_group_id, 
    updated_time DESC, 
    deleted
);

-- 模板搜索查询索引
CREATE INDEX idx_program_template_search ON t_program(
    org_id, 
    program_status, 
    name, 
    description, 
    deleted
);

-- 模板权限验证索引
CREATE INDEX idx_program_template_permission ON t_program(
    id, 
    org_id, 
    program_status, 
    deleted
);
```

#### 用户组继承查询索引
```sql
-- 用户组层级查询索引（如果还没有）
CREATE INDEX idx_user_group_hierarchy ON t_user_group(
    ugid, 
    path, 
    oid
);

-- 用户组父子关系索引
CREATE INDEX idx_user_group_parent ON t_user_group(
    parent, 
    oid
);
```

### 2. 性能分析

#### 查询性能预期
- **模板列表查询**：预期在1000万条记录下 < 100ms
- **用户组继承查询**：预期在10层用户组层级下 < 50ms
- **模板搜索查询**：预期在关键词搜索时 < 200ms

#### 索引覆盖率
- 主查询索引覆盖率：95%+
- 搜索查询索引覆盖率：90%+
- 权限验证索引覆盖率：100%

## 查询优化策略

### 1. 分页优化

#### 当前分页方式
```java
// 使用 MyBatis-Plus 的 Page 分页
Page<ProgramDO> pageParam = new Page<>(page, size);
```

#### 优化建议
```sql
-- 对于大数据集，考虑使用游标分页
SELECT * FROM t_program 
WHERE org_id = ? 
  AND program_status = 'TEMPLATE'
  AND updated_time < ?  -- 游标位置
ORDER BY updated_time DESC 
LIMIT ?;
```

### 2. 用户组继承查询优化

#### 当前实现
```java
// 通过 UserGroupRepository.getAllUgidsByParent() 获取继承的用户组
List<Long> inheritedUgids = userGroupRepository.getAllUgidsByParent(ugid);
```

#### 优化方案

##### 方案一：缓存优化
```java
@Cacheable(value = "userGroupInheritance", key = "#ugid")
public List<Long> getAllUgidsByParentCached(Long ugid) {
    return userGroupRepository.getAllUgidsByParent(ugid);
}
```

##### 方案二：预计算继承关系
```sql
-- 创建用户组继承关系表
CREATE TABLE t_user_group_inheritance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ancestor_ugid BIGINT NOT NULL,
    descendant_ugid BIGINT NOT NULL,
    path_length INT NOT NULL,
    org_id BIGINT NOT NULL,
    INDEX idx_inheritance_ancestor (ancestor_ugid, org_id),
    INDEX idx_inheritance_descendant (descendant_ugid, org_id)
);
```

### 3. 全文搜索优化

#### 当前搜索实现
```java
// 使用 LIKE 查询进行关键词搜索
queryWrapper.like("name", keyword).or().like("description", keyword);
```

#### 优化建议
```sql
-- 如果搜索需求复杂，考虑使用全文索引
ALTER TABLE t_program ADD FULLTEXT(name, description);

-- 全文搜索查询
SELECT * FROM t_program 
WHERE MATCH(name, description) AGAINST(? IN NATURAL LANGUAGE MODE)
  AND org_id = ? 
  AND program_status = 'TEMPLATE';
```

## 架构优化建议

### 1. 数据分离策略

#### MongoDB 内容存储优化
```java
// 考虑按组织ID进行分片
@Document(collection = "program_content_#{@organizationShardingStrategy.getShardSuffix(#root.orgId)}")
public class ProgramContent {
    // 内容字段
}
```

#### 读写分离
```yaml
# 配置读写分离
spring:
  datasource:
    master:
      url: jdbc:mysql://master-db:3306/led_platform
    slave:
      url: jdbc:mysql://slave-db:3306/led_platform
```

### 2. 缓存策略

#### 多级缓存设计
```java
@Service
public class TemplateCacheService {
    
    // L1缓存：本地缓存（Caffeine）
    @Cacheable(value = "templateListL1", key = "#oid + '_' + #ugid")
    public PageVO<ProgramDTO> getTemplateListFromL1Cache(Long oid, Long ugid, String keyword, int page, int size) {
        return null; // 缓存未命中
    }
    
    // L2缓存：分布式缓存（Redis）
    @Cacheable(value = "templateListL2", key = "#oid + '_' + #ugid", unless = "#result == null")
    public PageVO<ProgramDTO> getTemplateListFromL2Cache(Long oid, Long ugid, String keyword, int page, int size) {
        return programService.findTemplatesWithInheritance(oid, ugid, keyword, page, size);
    }
}
```

#### 缓存失效策略
```java
@CacheEvict(value = {"templateListL1", "templateListL2"}, allEntries = true)
public void evictTemplateCaches() {
    log.info("Template caches evicted");
}
```

### 3. 异步处理优化

#### 素材依赖异步处理
```java
@Async("templateTaskExecutor")
public CompletableFuture<Void> createMaterialReferencesAsync(Program template, String contentData) {
    try {
        materialDependencyService.createMaterialDependencies(
            template.getId(), contentData, template.getOid());
        return CompletableFuture.completedFuture(null);
    } catch (Exception e) {
        return CompletableFuture.failedFuture(e);
    }
}
```

## 监控和性能指标

### 1. 关键性能指标 (KPI)

#### 响应时间目标
- 模板列表查询：P95 < 200ms, P99 < 500ms
- 模板创建：P95 < 1000ms, P99 < 2000ms
- 模板更新：P95 < 800ms, P99 < 1500ms
- 模板搜索：P95 < 300ms, P99 < 800ms

#### 并发性能目标
- 支持 1000 QPS 的模板查询
- 支持 100 QPS 的模板创建/更新
- 支持 500 QPS 的模板搜索

### 2. 监控告警

#### 数据库性能监控
```yaml
# 慢查询监控
slow_query_log: ON
long_query_time: 0.1  # 100ms

# 索引使用监控
log_queries_not_using_indexes: ON
```

#### 应用性能监控
```java
// 使用 Micrometer 进行性能监控
@Timed(name = "template.query.duration", description = "Template query duration")
public PageVO<ProgramDTO> findTemplatesWithInheritance(Long oid, Long ugid, String keyword, int page, int size) {
    // 实现
}
```

## 实施计划

### 阶段一：基础优化（1周）
1. ✅ 添加基础索引
2. ✅ 实现错误处理和回滚机制
3. ✅ 完善参数验证

### 阶段二：性能优化（2周）
1. 🔄 实现缓存策略
2. 🔄 优化查询性能
3. 🔄 添加性能监控

### 阶段三：高级优化（3周）
1. ⏸️ 实现分片策略
2. ⏸️ 优化异步处理
3. ⏸️ 完善监控告警

## 总结

通过以上优化措施，预期可以实现：

1. **性能提升**：查询性能提升 60-80%
2. **可扩展性**：支持更大规模的数据和并发
3. **稳定性**：完善的错误处理和监控机制
4. **可维护性**：清晰的架构设计和文档

建议按照实施计划分阶段推进，首先确保基础功能稳定，然后逐步优化性能和扩展性。
# 缓存Key命名规范

## 概述

为了支持微服务间的缓存共享，Redis缓存不使用服务级别的前缀。所有服务遵循统一的key命名规范，确保缓存数据的一致性和可访问性。

## 命名规范

### 基本格式
```
{业务域}:{数据类型}:{主键}[:{子键}]
```

### 命名约定
- 使用小写字母和下划线
- 使用冒号(:)作为层级分隔符
- 避免使用特殊字符和空格
- 保持key长度合理（建议不超过100字符）

## 业务域分类

### 用户相关(user)
```
user:profile:{userId}              # 用户基本信息
user:permissions:{userId}          # 用户权限信息
user:groups:{userId}              # 用户所属组
user:online:{userId}              # 用户在线状态
user:session:{sessionId}          # 用户会话信息
user:preferences:{userId}         # 用户偏好设置
```

### 组织相关(org)
```
org:info:{orgId}                  # 组织基本信息
org:users:{orgId}                 # 组织用户列表
org:permissions:{orgId}           # 组织权限配置
org:config:{orgId}                # 组织配置信息
```

### 设备相关(device)
```
device:info:{deviceId}            # 设备基本信息
device:status:{deviceId}          # 设备状态
device:online:{deviceId}          # 设备在线状态
device:config:{deviceId}          # 设备配置
device:groups:{deviceId}          # 设备分组信息
```

### 终端组相关(terminal)
```
terminal:group:info:{groupId}         # 终端组信息
terminal:group:permissions:{groupId}  # 终端组权限
terminal:group:devices:{groupId}      # 终端组设备列表
```

### 权限相关(permission)
```
permission:expression:{expressionId}  # 权限表达式
permission:cache:{userId}:{resource}  # 用户权限缓存
permission:roles:{userId}             # 用户角色信息
```

### 消息相关(message)
```
message:template:{templateId}         # 消息模板
message:user:{userId}                # 用户消息
message:unread:{userId}              # 未读消息计数
message:history:{conversationId}     # 消息历史
```

### 系统相关(system)
```
system:config:{configKey}           # 系统配置
system:dict:{dictType}:{dictKey}     # 数据字典
system:cache:{cacheType}            # 系统级缓存
```

### 统计相关(stats)
```
stats:daily:{date}:{type}           # 日统计数据
stats:user:{userId}:{metric}        # 用户统计
stats:device:{deviceId}:{metric}    # 设备统计
```

## 组织隔离

对于需要组织隔离的数据，使用以下格式：
```
org:{orgId}:{业务域}:{数据类型}:{主键}
```

示例：
```
org:1001:user:profile:123           # 组织1001下的用户123信息
org:1001:device:status:456          # 组织1001下的设备456状态
org:1002:message:template:789       # 组织1002的消息模板789
```

## TTL建议

### 短期缓存 (5-30分钟)
- 用户在线状态
- 设备实时状态
- 临时会话数据

### 中期缓存 (30分钟-2小时)
- 用户基本信息
- 权限数据
- 设备配置

### 长期缓存 (2-24小时)
- 组织信息
- 系统配置
- 数据字典
- 消息模板

### 持久缓存 (不过期或长期)
- 静态配置数据
- 字典数据
- 模板数据

## 最佳实践

### 1. Key设计原则
- **简洁明确**: key应该一眼就能看出存储的是什么数据
- **层次分明**: 使用冒号分隔不同层级
- **易于管理**: 支持通配符批量操作

### 2. 避免的做法
- 避免在key中包含动态时间戳
- 避免使用过长的key
- 避免在key中包含敏感信息
- 避免使用非ASCII字符

### 3. 批量操作
使用通配符进行批量清理：
```
user:*                    # 清理所有用户相关缓存
org:1001:*               # 清理组织1001的所有缓存
device:status:*          # 清理所有设备状态缓存
```

### 4. 监控建议
- 监控热点key的访问频率
- 定期检查key的TTL设置
- 监控缓存命中率
- 定期清理过期和无效的key

## 各服务职责

### Core-Service
- 管理用户、组织、权限相关缓存
- 负责系统配置和字典缓存
- 提供缓存工具类和统一接口

### Message-Service  
- 管理消息模板和消息历史缓存
- 可读取用户信息缓存
- 管理用户在线状态缓存

### Auth-Server
- 管理用户会话和权限缓存
- 可读取用户基本信息缓存
- 管理认证相关临时数据

### Gateway
- 管理路由和限流相关缓存
- 可读取用户和权限缓存
- 管理API调用统计缓存

## 注意事项

1. **数据一致性**: 修改缓存数据时需要考虑其他服务的影响
2. **清理策略**: 需要明确哪个服务负责清理哪类缓存
3. **版本兼容**: 数据结构变更时要考虑向后兼容性
4. **异常处理**: 缓存不可用时要有降级方案

## 示例代码

### CacheService使用示例
```java
// 获取用户信息
UserInfo user = cacheService.get("user:profile:" + userId, UserInfo.class);

// 存储设备状态，TTL 30分钟
cacheService.put("device:status:" + deviceId, status, Duration.ofMinutes(30));

// 批量清理用户相关缓存
cacheService.evictByPattern("user:*:" + userId);

// 组织隔离的缓存
String orgUserKey = "org:" + orgId + ":user:profile:" + userId;
cacheService.put(orgUserKey, userInfo);
```

### @Cacheable使用示例
```java
@Cacheable(cacheNames = "user-profile", key = "'user:profile:' + #userId")
public UserInfo getUserProfile(Long userId) {
    // 数据库查询逻辑
}

@CacheEvict(cacheNames = "user-profile", key = "'user:profile:' + #userId")
public void updateUserProfile(Long userId, UserInfo userInfo) {
    // 更新逻辑
}
```

遵循这些规范可以确保微服务间缓存的一致性和可维护性。
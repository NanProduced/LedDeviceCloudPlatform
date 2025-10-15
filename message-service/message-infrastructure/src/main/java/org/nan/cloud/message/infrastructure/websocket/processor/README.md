# 业务消息处理器架构

## 概述

业务消息处理器架构是Phase 2.2的核心实现，它提供了一个灵活的、基于策略模式的消息处理框架，用于处理来自RabbitMQ的各种业务消息并转换为STOMP消息推送给前端。

## 架构设计

### 核心组件

1. **BusinessMessageProcessor** - 业务消息处理器接口
   - 定义统一的消息处理契约
   - 支持消息类型判断和优先级设置
   - 返回标准化的处理结果

2. **BusinessMessageProcessorManager** - 业务消息处理器管理器
   - 自动发现和注册所有处理器实现
   - 按优先级排序和路由选择
   - 提供统一的处理入口

3. **具体处理器实现**
   - **CommandMessageProcessor** - 指令消息处理器
   - **StatusMessageProcessor** - 状态消息处理器  
   - **NotificationMessageProcessor** - 通知消息处理器

### 设计原则

- **单一职责原则** - 每个处理器只处理特定类型的业务消息
- **开闭原则** - 支持扩展新的业务消息类型，不修改现有代码
- **依赖倒置原则** - 依赖抽象接口，不依赖具体实现
- **向后兼容** - 保留原有转换器作为降级备选方案

## 消息处理流程

```
MQ消息 → MqStompBridgeListener → BusinessMessageProcessorManager → 选择处理器 → 转换STOMP消息 → StompMessageDispatcher → 分发到前端
```

### 详细流程

1. **消息接收** - MqStompBridgeListener接收RabbitMQ消息
2. **处理器选择** - BusinessMessageProcessorManager根据消息类型和路由键选择合适的处理器
3. **消息处理** - 具体处理器解析消息，转换为STOMP格式
4. **消息分发** - 使用StompMessageDispatcher进行智能分发
5. **降级处理** - 如果新处理器失败，自动降级到原有转换器逻辑

## 支持的消息类型

### 指令消息 (CommandMessageProcessor)
- **COMMAND_RESULT** - 单个指令执行结果
- **BATCH_COMMAND_PROGRESS** - 批量指令执行进度
- **COMMAND_ERROR** - 指令执行错误

路由键模式：
- `stomp.command.result.{orgId}.{userId}`
- `stomp.batch.progress.{userId}.{batchId}`
- `stomp.command.error.{orgId}.{deviceId}`

### 状态消息 (StatusMessageProcessor)
- **DEVICE_STATUS** - 设备状态变更
- **USER_STATUS** - 用户状态变更
- **SERVICE_STATUS** - 服务状态变更
- **CONNECTION_STATUS** - 连接状态变更

路由键模式：
- `stomp.device.status.{orgId}.{deviceId}`
- `stomp.user.status.{orgId}.{userId}`
- `stomp.service.status.{serviceId}`
- `stomp.connection.status.{connectionType}.{resourceId}`

### 通知消息 (NotificationMessageProcessor)
- **SYSTEM_NOTIFICATION** - 系统级通知
- **BUSINESS_NOTIFICATION** - 业务级通知
- **USER_NOTIFICATION** - 用户级通知
- **ORG_NOTIFICATION** - 组织级通知

路由键模式：
- `stomp.system.notification.{type}.{priority}`
- `stomp.business.notification.{businessType}.{orgId}`
- `stomp.user.notification.{userId}.{type}`
- `stomp.org.notification.{orgId}.{type}`

## 使用示例

### 创建自定义处理器

```java
@Component
public class CustomMessageProcessor implements BusinessMessageProcessor {
    
    @Override
    public String getSupportedMessageType() {
        return "CUSTOM";
    }
    
    @Override
    public boolean supports(String messageType, String routingKey) {
        return "CUSTOM_MESSAGE".equalsIgnoreCase(messageType) ||
               routingKey.startsWith("stomp.custom.");
    }
    
    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }
    
    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey, 
                                              Map<String, Object> messageHeaders) {
        // 自定义处理逻辑
        // ...
        return BusinessMessageProcessResult.success(messageId, dispatchResult, stompMessage);
    }
}
```

### 消息格式示例

#### 指令结果消息
```json
{
    "commandId": "cmd-123",
    "deviceId": "device-456",
    "orgId": 1,
    "userId": 100,
    "result": "SUCCESS",
    "status": "COMPLETED",
    "resultData": {
        "executionTime": 1500,
        "returnCode": 0
    },
    "timestamp": 1640995200000
}
```

#### 设备状态消息
```json
{
    "deviceId": "device-456",
    "orgId": 1,
    "status": "ONLINE",
    "previousStatus": "OFFLINE",
    "statusType": "CONNECTION",
    "statusData": {
        "ip": "192.168.1.100",
        "lastHeartbeat": 1640995200000
    },
    "timestamp": 1640995200000,
    "reason": "Device reconnected"
}
```

#### 系统通知消息
```json
{
    "notificationId": "notif-789",
    "notificationType": "MAINTENANCE",
    "title": "系统维护通知",
    "content": "系统将于今晚进行维护",
    "priority": "HIGH",
    "category": "MAINTENANCE",
    "targetRoles": ["ADMIN", "MANAGER"],
    "expireTime": 1641081600000
}
```

## 配置说明

### 处理器优先级
- **10** - 高优先级（指令消息）
- **20** - 中等优先级（状态消息）
- **30** - 较低优先级（通知消息）
- **100** - 默认优先级

### 消息TTL配置
- **指令结果** - 5分钟
- **设备状态** - 30秒
- **批量进度** - 3分钟
- **系统通知** - 24小时
- **用户通知** - 3天

## 监控和调试

### 日志级别
- **DEBUG** - 消息接收和处理器选择
- **INFO** - 处理成功和分发结果
- **WARN** - 处理失败和降级
- **ERROR** - 异常和错误

### 关键日志标识
- ✅ - 处理成功
- ⬇️ - 降级处理
- ⚠️ - 警告信息
- ❌ - 处理失败
- 💥 - 异常错误

### 性能监控
- 处理器注册数量
- 消息处理成功率
- 降级处理频率
- 分发结果统计

## 扩展点

1. **新增处理器** - 实现BusinessMessageProcessor接口
2. **自定义路由** - 重写supports方法
3. **优先级调整** - 重写getPriority方法
4. **分发策略** - 使用不同的StompMessageDispatcher方法

## 最佳实践

1. **错误处理** - 总是提供有意义的错误信息
2. **性能考虑** - 避免阻塞操作，使用异步处理
3. **幂等性** - 确保重复处理同一消息不会产生副作用
4. **监控告警** - 设置适当的监控指标和告警阈值
5. **测试覆盖** - 为每个处理器编写充分的单元测试

## 注意事项

1. **向后兼容** - 新的处理器架构不会影响现有功能
2. **降级机制** - 处理器失败时会自动降级到原有逻辑
3. **线程安全** - 所有处理器都必须是线程安全的
4. **资源管理** - 正确管理数据库连接、网络连接等资源
5. **异常传播** - 合理处理异常，避免影响消息消费
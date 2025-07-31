# Phase 3.1: STOMP消息确认和重试机制

## 🎯 实现目标

Phase 3.1 成功实现了企业级的STOMP消息可靠性保障机制，提供了完整的消息确认、重试和监控能力，确保关键业务消息的可靠投递。

## 🏗️ 核心架构

### 1. MessageDeliveryTracker - 消息投递跟踪器
**文件**: `MessageDeliveryTracker.java`

完整的消息投递生命周期管理：
- **唯一ID生成**: 自动生成全局唯一的消息ID
- **投递状态跟踪**: PENDING → ACKNOWLEDGED/REJECTED/TIMEOUT → RETRYING → COMPLETED/FAILED
- **超时检测**: 可配置的消息投递超时机制
- **确认处理**: 支持ACK/NACK消息确认
- **统计监控**: 完整的投递成功率、重试次数等统计

```java
// 核心API
public String generateMessageId()
public void startTracking(String messageId, String destination, String userId, String messageType, String content)
public boolean acknowledgeMessage(String messageId, String userId)
public boolean rejectMessage(String messageId, String userId, String reason)
public void handleTimeout(String messageId)
```

**投递状态模型**:
```java
public enum DeliveryStatus {
    PENDING("待投递"),     // 初始状态
    ACKNOWLEDGED("已确认"), // 客户端确认成功
    REJECTED("已拒绝"),     // 客户端拒绝，可能重试
    TIMEOUT("超时"),       // 投递超时
    RETRYING("重试中"),    // 正在重试
    FAILED("最终失败")     // 重试失败，最终放弃
}
```

### 2. RetryConfiguration - 重试策略配置
**文件**: `RetryConfiguration.java`

差异化重试策略管理：

#### 预定义策略
- **CRITICAL**: 关键消息 - 5次重试，60秒超时，指数退避1.5倍
- **NORMAL**: 普通消息 - 3次重试，30秒超时，指数退避2.0倍
- **LOW_PRIORITY**: 低优先级 - 1次重试，15秒超时，无退避
- **BATCH**: 批量消息 - 2次重试，45秒超时，指数退避2.5倍
- **SYSTEM_NOTIFICATION**: 系统通知 - 4次重试，25秒超时，指数退避1.8倍
- **DEVICE_COMMAND**: 设备指令 - 3次重试，40秒超时，指数退避2.2倍
- **STATUS_UPDATE**: 状态更新 - 2次重试，20秒超时，指数退避2.0倍

#### 指数退避算法
```java
public long calculateDelay(int attemptNumber) {
    // delay = initial * (multiplier ^ attempts)
    double delay = initialDelaySeconds * Math.pow(backoffMultiplier, attemptNumber);
    return Math.min((long) delay, maxDelaySeconds);
}
```

### 3. StompAckHandler - STOMP确认处理器
**文件**: `StompAckHandler.java`

客户端确认消息的统一处理器：

#### 支持的确认类型
- **MESSAGE_ACK**: 消息确认（成功接收）
- **MESSAGE_NACK**: 消息拒绝（接收失败，需要重试）
- **HEARTBEAT**: 客户端心跳确认
- **SUBSCRIPTION_ACK**: 订阅确认

#### 核心功能
```java
@MessageMapping("/ack")
public void handleMessageAck(@Payload AckMessage ackMessage, 
                            SimpMessageHeaderAccessor headerAccessor,
                            Principal principal)

@SubscribeMapping("/queue/ack")
public void handleSubscription(SimpMessageHeaderAccessor headerAccessor, Principal principal)
```

#### 连接状态管理
```java
@Data
public static class ClientConnectionStatus {
    private String userId;
    private boolean isActive;
    private LocalDateTime connectedAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime lastHeartbeatAt;
    private int totalMessages;
    private int acknowledgedMessages;
    private int rejectedMessages;
    
    public double getAckRate() // 确认率计算
}
```

### 4. ReliableMessageSender - 可靠性消息发送服务
**文件**: `ReliableMessageSender.java`

集成确认和重试的高级消息发送服务：

#### 核心特性
- **自动ID生成**: 确保每条消息都有唯一标识
- **投递跟踪集成**: 自动启动投递跟踪机制
- **智能重试**: 基于消息类型的差异化重试策略
- **异步处理**: 非阻塞的重试机制
- **事件驱动**: 监听确认事件，自动取消重试

#### 主要API
```java
public MessageSendResult sendReliableMessage(String userId, String destination, 
                                           CommonStompMessage message, boolean requiresAck)

public MessageSendResult sendReliableTopicMessage(String topic, CommonStompMessage message, 
                                                boolean requiresAck)

@EventListener
public void handleAckEvent(StompAckHandler.AckEvent event) // 自动取消已确认消息的重试
```

### 5. ReliabilityController - 可靠性管理API
**文件**: `ReliabilityController.java`

完整的可靠性功能REST API：

#### API分类
- **投递跟踪API**: `/api/v1/message/reliability/delivery/*`
- **重试策略API**: `/api/v1/message/reliability/retry/*`
- **连接状态API**: `/api/v1/message/reliability/connection/*`
- **可靠性发送API**: `/api/v1/message/reliability/send/*`
- **维护管理API**: `/api/v1/message/reliability/maintenance/*`

#### 关键接口
```http
GET    /api/v1/message/reliability/delivery/stats           # 获取投递统计
GET    /api/v1/message/reliability/delivery/pending-count   # 待确认消息数
GET    /api/v1/message/reliability/delivery/record/{id}     # 投递记录详情
POST   /api/v1/message/reliability/delivery/acknowledge/{id} # 手动确认消息

GET    /api/v1/message/reliability/retry/policies           # 所有重试策略
PUT    /api/v1/message/reliability/retry/policy/{type}      # 设置重试策略
DELETE /api/v1/message/reliability/retry/policy/{type}      # 移除重试策略

GET    /api/v1/message/reliability/connection/status/{user} # 连接状态
GET    /api/v1/message/reliability/connection/active-count  # 活跃连接数

POST   /api/v1/message/reliability/send/user                # 可靠性用户消息
POST   /api/v1/message/reliability/send/topic               # 可靠性主题消息

POST   /api/v1/message/reliability/maintenance/cleanup      # 清理过期数据
```

### 6. ReliabilityConfig - 配置和定时任务
**文件**: `ReliabilityConfig.java`

自动化维护和监控配置：

#### 定时任务
- **清理过期投递记录**: 每小时执行，清理24小时前的记录
- **清理过期连接状态**: 每30分钟执行，清理60分钟无活动的连接
- **清理已完成重试任务**: 每15分钟执行，清理已完成的重试任务
- **输出可靠性统计**: 每10分钟执行，输出系统运行统计

#### 配置项支持
```yaml
message:
  reliability:
    enabled: true                    # 启用可靠性功能
    cleanup:
      enabled: true                  # 启用自动清理
      delivery-record-retention-hours: 24
      connection-timeout-minutes: 60
    monitoring:
      enabled: true                  # 启用监控统计
      stats-interval-minutes: 10
    retry:
      default-max-retries: 3
      default-initial-delay-seconds: 5
      default-max-delay-seconds: 300
```

## 🔄 集成策略

### 与现有架构的完美集成

#### 1. 保持现有API兼容性
- **StompMessageSender**: 保持原有API不变，新增可靠性功能为可选
- **向后兼容**: 现有代码无需修改即可继续工作
- **渐进式升级**: 可以逐步将关键消息改用可靠性发送

#### 2. 复用现有基础设施
- **利用现有STOMP配置**: 复用WebSocketStompConfig和相关拦截器
- **集成现有安全机制**: 复用GatewayAuthValidator和用户身份验证
- **复用现有消息模型**: 扩展CommonStompMessage，保持一致性

#### 3. 事件驱动集成
- **发布确认事件**: 与现有事件系统集成，支持自定义事件处理
- **监听系统事件**: 可以监听业务事件，提供可靠性支持

## 🚀 性能特性

### 1. 高性能设计
- **异步处理**: 所有重试操作都是异步执行，不阻塞主流程
- **内存优化**: 定时清理过期数据，防止内存泄漏
- **批量操作**: 支持批量清理和统计操作

### 2. 可扩展性
- **插件化重试策略**: 支持自定义重试策略
- **可配置阈值**: 所有超时和重试参数都可配置
- **监控友好**: 提供丰富的统计指标

### 3. 可靠性保障
- **多重超时机制**: 消息投递超时、重试超时、连接超时
- **故障隔离**: 单个消息失败不影响其他消息
- **优雅降级**: 可靠性功能失败时自动降级到普通发送

## 📊 监控能力

### 实时统计指标
```
📊 STOMP可靠性统计 - 
活跃连接: 156, 待确认: 23, 待重试: 5, 
总发送: 12847, 已确认: 12456, 成功率: 97.0%, 
超时: 285, 失败: 106, 重试: 391
```

### 详细投递记录
- 每条消息的完整投递历史
- 重试次数和失败原因
- 投递耗时统计
- 用户确认行为分析

## 🧪 使用示例

### 1. 客户端确认消息
```javascript
// 客户端订阅确认队列
stompClient.subscribe('/user/queue/ack', function(frame) {
    console.log('订阅确认队列成功');
});

// 发送消息确认
stompClient.send('/app/ack', {}, JSON.stringify({
    messageId: 'msg_1690789234567_123',
    ackType: 'MESSAGE_ACK',
    timestamp: new Date().toISOString()
}));

// 发送消息拒绝
stompClient.send('/app/ack', {}, JSON.stringify({
    messageId: 'msg_1690789234567_124',
    ackType: 'MESSAGE_NACK',
    reason: 'Invalid message format',
    timestamp: new Date().toISOString()
}));

// 发送心跳
stompClient.send('/app/ack', {}, JSON.stringify({
    ackType: 'HEARTBEAT',
    timestamp: new Date().toISOString()
}));
```

### 2. 服务端可靠性发送
```java
// 发送需要确认的关键消息
CommonStompMessage criticalMessage = CommonStompMessage.builder()
    .messageType("CRITICAL")
    .content("重要系统通知")
    .priority(Priority.HIGH)
    .build();

MessageSendResult result = reliableMessageSender.sendReliableMessage(
    "user123", "/queue/notifications", criticalMessage, true);

// 发送不需要确认的普通消息
CommonStompMessage normalMessage = CommonStompMessage.builder()
    .messageType("NORMAL")
    .content("一般信息更新")
    .build();

reliableMessageSender.sendReliableMessage(
    "user456", "/queue/updates", normalMessage, false);
```

### 3. 自定义重试策略
```java
// 为VIP用户设置特殊重试策略
RetryConfiguration.RetryPolicy vipPolicy = RetryConfiguration.RetryPolicy.builder()
    .maxRetries(10)              // VIP用户最多重试10次
    .initialDelaySeconds(1)      // 初始延迟1秒
    .maxDelaySeconds(120)        // 最大延迟2分钟
    .backoffMultiplier(1.2)      // 较小的退避倍数
    .timeoutSeconds(90)          // 90秒超时
    .retryOnTimeout(true)
    .retryOnReject(true)
    .build();

retryConfiguration.setRetryPolicy("VIP_MESSAGE", vipPolicy);
```

### 4. 监控和管理
```bash
# 查看投递统计
curl http://localhost:8084/api/v1/message/reliability/delivery/stats

# 查看待确认消息数量
curl http://localhost:8084/api/v1/message/reliability/delivery/pending-count

# 查看活跃连接数
curl http://localhost:8084/api/v1/message/reliability/connection/active-count

# 手动清理过期数据
curl -X POST http://localhost:8084/api/v1/message/reliability/maintenance/cleanup
```

## ✅ Phase 3.1 完成确认

**已完成的核心功能**:
- ✅ 消息投递跟踪和生命周期管理
- ✅ 多样化重试策略和指数退避算法
- ✅ 客户端确认机制（ACK/NACK/HEARTBEAT）
- ✅ 可靠性消息发送服务
- ✅ 完整的REST API管理接口
- ✅ 自动化清理和监控任务
- ✅ 与现有架构的无缝集成
- ✅ 丰富的配置选项和统计监控

**技术亮点**:
- 🎯 **高可靠性**: 多重超时机制、智能重试、确认保障
- 🚀 **高性能**: 异步处理、内存优化、批量操作
- 🔧 **易集成**: 向后兼容、渐进式升级、事件驱动
- 📊 **可观测**: 详细统计、实时监控、完整日志
- 🛡️ **故障隔离**: 单点失败不影响整体、优雅降级

**Phase 3.1: STOMP消息确认和重试机制** 现已完整实现！

这个可靠性层为整个消息系统提供了企业级的消息投递保障，通过完善的确认机制、智能重试策略和全面的监控能力，确保关键业务消息能够可靠地投递到目标用户，大幅提升了系统的可靠性和用户体验。
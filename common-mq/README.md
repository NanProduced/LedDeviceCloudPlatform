# 通用RabbitMQ客户端模块

这是一个为LedDeviceCloudPlatform项目设计的通用RabbitMQ客户端封装模块，提供简单易用的消息队列操作接口，支持其他服务快速接入和使用RabbitMQ。

## 模块架构

### 三层分离设计

- **common-mq-core**: 核心抽象和通用组件
  - 消息抽象 (`Message`)
  - 配置属性 (`MqProperties`)
  - 异常定义 (`MqException`)
  - 序列化器 (`MessageSerializer`)

- **common-mq-producer**: 消息生产者模块
  - 消息发送接口 (`MessageProducer`)
  - 发送结果封装 (`SendResult`, `BatchSendResult`)
  - 统计和监控 (`ProducerStats`, `HealthStatus`)

- **common-mq-consumer**: 消息消费者模块
  - 消息消费接口 (`MessageConsumer`)
  - 消费结果处理
  - 错误处理和重试机制

## 设计优势

### 1. 最小粒度引用
```xml
<!-- 只需要发送消息的服务 -->
<dependency>
    <groupId>org.nan</groupId>
    <artifactId>common-mq-producer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 只需要消费消息的服务 -->
<dependency>
    <groupId>org.nan</groupId>
    <artifactId>common-mq-consumer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>

<!-- 既需要发送又需要消费的服务 -->
<dependency>
    <groupId>org.nan</groupId>
    <artifactId>common-mq-producer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.nan</groupId>
    <artifactId>common-mq-consumer</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 统一的消息抽象
```java
// 创建通知消息
Message notification = Message.notification(
    "系统维护通知", 
    "系统将于今晚进行维护", 
    "user123", 
    "org456"
);

// 创建事件消息
Message event = Message.event(
    "USER_LOGIN", 
    loginData, 
    "auth-service", 
    "message-service"
);
```

### 3. 简化的API接口
```java
@Autowired
private MessageProducer messageProducer;

// 同步发送
SendResult result = messageProducer.send(message);

// 异步发送
CompletableFuture<SendResult> future = messageProducer.sendAsync(message);

// 批量发送
BatchSendResult batchResult = messageProducer.sendBatch(messages);

// 便捷方法
SendResult result = messageProducer.sendNotification(
    "订单完成", orderData, "user123", "org456"
);
```

## 核心特性

### 🚀 高性能
- 异步发送支持
- 批量发送优化
- 连接池复用
- 消息压缩支持

### 🛡️ 高可靠性
- 发布确认机制
- 死信队列处理
- 自动重试机制
- 异常详细分类

### 📊 可观测性
- 详细的统计信息
- 健康状态检查
- 慢消息检测
- 性能指标收集

### 🔧 易扩展性
- 插件化序列化器
- 自定义路由策略
- 灵活的配置选项
- 多种消息类型支持

## 配置说明

```yaml
nan:
  mq:
    enabled: true
    application-name: your-service
    
    # 生产者配置
    producer:
      confirm-enabled: true
      returns-enabled: true
      send-timeout: 30s
      retry-enabled: true
      max-retry-attempts: 3
      batch-size: 100
      
    # 消费者配置  
    consumer:
      concurrency: 1
      max-concurrency: 3
      prefetch-count: 5
      acknowledge-mode: manual
      
    # 死信配置
    dead-letter:
      enabled: true
      exchange-name: dlx.exchange
      message-ttl: 86400000  # 24小时
      
    # 监控配置
    monitor:
      enabled: true
      metrics-enabled: true
      slow-message-threshold: 5s
      
    # 序列化配置
    serialization:
      type: json
      compression-enabled: false
      compression-threshold: 1024
```

## 快速开始

### 1. 添加依赖
根据需要选择相应的依赖模块（如上所示）。

### 2. 配置RabbitMQ
在 `application.yml` 中配置RabbitMQ连接信息：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    
nan:
  mq:
    enabled: true
    application-name: your-service-name
```

### 3. 发送消息
```java
@Service
public class NotificationService {
    
    @Autowired
    private MessageProducer messageProducer;
    
    public void sendUserNotification(String userId, String content) {
        SendResult result = messageProducer.sendNotification(
            "用户通知", 
            content, 
            userId, 
            getCurrentOrgId()
        );
        
        if (!result.isSuccess()) {
            log.error("通知发送失败: {}", result.getErrorMessage());
        }
    }
    
    public void sendBatchNotifications(List<String> userIds, String content) {
        List<Message> messages = userIds.stream()
            .map(userId -> Message.notification("批量通知", content, userId, getCurrentOrgId()))
            .collect(Collectors.toList());
            
        BatchSendResult result = messageProducer.sendBatch(messages);
        log.info("批量发送结果: {}", result.getSummary());
    }
}
```

### 4. 消费消息
```java
@Component
public class NotificationConsumer {
    
    @RabbitListener(queues = "notification.queue")
    public void handleNotification(Message message, Channel channel, 
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到通知消息: {}", message.getDescription());
            
            // 处理业务逻辑
            processNotification(message);
            
            // 手动确认
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("处理通知消息失败: {}", e.getMessage(), e);
            
            try {
                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息确认失败", ioException);
            }
        }
    }
    
    private void processNotification(Message message) {
        // 实现具体的业务逻辑
    }
}
```

## 监控和统计

### 获取生产者统计信息
```java
@RestController
public class MqStatsController {
    
    @Autowired
    private MessageProducer messageProducer;
    
    @GetMapping("/mq/producer/stats")
    public ProducerStats getProducerStats() {
        return messageProducer.getStats();
    }
    
    @GetMapping("/mq/producer/health") 
    public HealthStatus getProducerHealth() {
        return messageProducer.getHealth();
    }
}
```

### 统计信息示例
```json
{
  "totalSentCount": 1000,
  "successSentCount": 995,
  "failedSentCount": 5,
  "averageSendDuration": 23.5,
  "maxSendDuration": 120,
  "minSendDuration": 8,
  "currentSendRate": 45.2,
  "totalRetryCount": 12,
  "successRate": 99.5
}
```

## 最佳实践

### 1. 消息设计
- 使用有意义的主题（subject）
- 保持消息payload轻量化
- 合理设置消息优先级
- 包含必要的上下文信息

### 2. 性能优化
- 合理使用批量发送
- 启用消息压缩（大消息）
- 配置合适的预取数量
- 避免长时间阻塞消费者

### 3. 错误处理
- 实现幂等性消费
- 合理设置重试策略
- 监控死信队列
- 记录详细的错误日志

### 4. 监控告警
- 监控消息积压情况
- 关注发送成功率
- 设置慢消息告警
- 定期检查连接状态

## 项目集成示例

参考 `message-service` 模块中的使用方式，了解如何在实际项目中集成这个通用MQ客户端模块。

## 版本兼容性

- Java 17+
- Spring Boot 3.3.11+
- Spring Cloud 2023.0.5+
- RabbitMQ 3.8+

## 贡献指南

1. 遵循现有的代码风格
2. 添加单元测试
3. 更新相关文档
4. 提交前运行完整测试

---

*这个模块设计遵循"按需引入、职责分离"的原则，为不同场景的服务提供最小化的依赖引入和最大化的功能覆盖。*
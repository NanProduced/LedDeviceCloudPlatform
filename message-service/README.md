# Message Service - 消息中心服务

## 📋 概述

Message Service 是 LedDeviceCloudPlatform 的统一消息中心服务，负责处理整个平台的实时消息推送、事件处理、WebSocket连接管理等功能。采用事件驱动架构，基于RabbitMQ消息队列，提供高可靠、高性能、可扩展的消息服务。

## 🚀 核心功能

- **🔌 WebSocket Hub**: 统一的WebSocket连接管理中心，支持原生WebSocket和STOMP协议
- **📬 消息推送**: 实时消息推送给在线用户，支持个人消息和组织广播
- **🗄️ 消息持久化**: 基于MongoDB的消息存储和历史记录管理
- **🎯 事件驱动**: 基于RabbitMQ的事件处理架构，支持异步事件分发
- **👥 多租户隔离**: 支持组织级别的消息隔离和权限控制
- **📊 统计分析**: 消息发送统计、用户在线状态分析和性能监控
- **🔄 重试机制**: 失败消息的自动重试和死信队列处理
- **🔐 认证集成**: 与Gateway认证体系无缝集成，支持CLOUD-AUTH头验证

## 🆕 最新更新功能

### STOMP WebSocket 支持
- **双协议支持**: 同时支持原生WebSocket和STOMP协议
- **多类型订阅**: 支持 `/topic/` 主题订阅和 `/user/` 个人消息
- **智能路由**: 自动根据用户身份和组织权限路由消息
- **连接管理**: 完整的连接生命周期管理和异常处理

### 增强的认证和安全
- **Gateway集成**: 完美集成Gateway的CLOUD-AUTH认证头
- **用户身份提取**: 自动提取用户ID、组织ID等身份信息
- **权限验证**: 基于用户权限控制消息接收范围
- **连接监控**: 实时监控连接状态和用户在线情况

### 实时测试和调试
- **STOMP测试页面**: 提供完整的STOMP协议测试界面
- **WebSocket测试**: 原生WebSocket连接测试工具
- **消息追踪**: 详细的消息发送和接收日志
- **性能监控**: 连接数量、消息吞吐量等实时指标

## 🏗️ 架构设计

### 服务分层架构

```
message-service/
├── message-api/          # API定义层
├── message-application/  # 应用逻辑层  
├── message-infrastructure/ # 基础设施层
└── message-boot/         # 启动配置层
```

### 技术栈

- **Spring Boot 3.3.11** - 基础框架
- **Spring WebSocket + STOMP** - WebSocket通信协议
- **RabbitMQ 3.12+** - 企业级消息队列
- **MongoDB 5.0+** - 消息持久化存储
- **Redis 6.0+** - 在线用户状态管理和会话缓存
- **Spring Cloud Stream** - 消息驱动微服务框架
- **Nacos** - 服务注册与配置中心

## 📂 模块结构详解

### 1. message-api - API定义模块

```
message-api/
├── dto/
│   ├── request/          # 请求DTO
│   ├── response/         # 响应DTO  
│   ├── websocket/        # WebSocket消息DTO
│   └── event/            # 事件DTO
├── enums/
│   ├── MessageType.java  # 消息类型枚举
│   ├── EventType.java    # 事件类型枚举
│   └── Priority.java     # 优先级枚举
├── service/              # 服务接口定义
├── event/                # 事件定义
│   ├── BaseEvent.java    # 基础事件
│   ├── DeviceEvent.java  # 设备事件
│   ├── UserEvent.java    # 用户事件
│   └── MessageEvent.java # 消息事件
└── constants/
    └── MessageRoutingKeys.java # 路由键常量
```

**职责**: 定义对外API接口、数据传输对象、事件模型、常量等

### 2. message-application - 应用逻辑模块

```
message-application/
├── service/              # 业务服务
│   ├── impl/
│   ├── MessageService.java         # 消息服务
│   ├── UserOnlinePushService.java  # 用户在线推送服务
│   ├── TaskResultNotificationService.java # 任务结果通知服务
│   └── UserOnlineStatusService.java # 用户在线状态服务
├── domain/               # 领域模型
│   ├── model/
│   │   └── TaskResultData.java     # 任务结果数据模型
│   ├── repository/       # 领域仓储接口
│   └── service/          # 领域服务接口
├── event/                # 领域事件
│   ├── handler/          # 事件处理器
│   ├── producer/         # 事件生产者
│   └── consumer/         # 事件消费者
└── utils/                # 工具类
    └── MessageUtils.java # 消息工具类
```

**职责**: 实现核心业务逻辑、领域模型定义、事件处理等

### 3. message-infrastructure - 基础设施模块

```
message-infrastructure/
├── mongodb/              # MongoDB相关
│   ├── document/         # 文档实体
│   │   ├── MessageDetail.java    # 消息详情文档
│   │   ├── TaskResult.java       # 任务结果文档
│   │   └── TemplateContent.java  # 模板内容文档
│   ├── repository/       # 仓储实现
│   └── config/           # MongoDB配置
├── redis/                # Redis相关
│   ├── manager/          # Redis管理器
│   │   └── MessageCacheManager.java # 消息缓存管理器
│   └── config/           # Redis配置
├── mq/                   # RabbitMQ相关
│   ├── config/           # RabbitMQ配置
│   │   └── RabbitMQConfig.java    # RabbitMQ配置类
│   ├── publisher/        # 消息发布者
│   │   └── MessageEventPublisher.java # 消息事件发布器
│   └── consumer/         # 消息消费者
│       ├── BusinessEventConsumer.java # 业务事件消费者
│       ├── DeviceEventConsumer.java   # 设备事件消费者
│       ├── UserEventConsumer.java     # 用户事件消费者
│       └── SystemMonitoringConsumer.java # 系统监控消费者
├── websocket/            # WebSocket相关
│   ├── config/           # WebSocket配置
│   │   ├── WebSocketConfig.java       # 原生WebSocket配置
│   │   └── WebSocketStompConfig.java  # STOMP协议配置
│   ├── controller/       # STOMP控制器
│   │   └── StompMessageController.java # STOMP消息控制器
│   ├── interceptor/      # 拦截器
│   │   ├── StompHandshakeInterceptor.java  # STOMP握手拦截器
│   │   ├── StompChannelInterceptor.java    # STOMP通道拦截器
│   │   └── StompPrincipal.java             # STOMP用户主体
│   ├── handler/          # 消息处理器
│   │   └── MessageWebSocketHandler.java    # WebSocket消息处理器
│   ├── manager/          # 会话管理器
│   │   └── WebSocketConnectionManager.java # 连接管理器
│   ├── security/         # 安全相关
│   │   ├── GatewayAuthValidator.java       # Gateway认证验证器
│   │   └── GatewayUserInfo.java            # Gateway用户信息
│   └── session/          # 会话管理
│       ├── WebSocketSessionInfo.java      # 会话信息
│       └── WebSocketSessionStore.java     # 会话存储
└── repository/           # 仓储实现
    ├── MessageEventRepositoryImpl.java    # 消息事件仓储实现
    ├── WebSocketConnectionRepositoryImpl.java # WebSocket连接仓储实现
    └── TaskResultPersistenceRepositoryImpl.java # 任务结果持久化仓储
```

**职责**: 提供数据访问、外部系统集成、WebSocket实现、RabbitMQ配置等基础设施

### 4. message-boot - 启动配置模块

```
message-boot/
├── controller/           # REST控制器
│   ├── MessageController.java             # 消息REST控制器
│   ├── UserOnlinePushController.java      # 用户在线推送控制器
│   ├── TaskNotificationController.java    # 任务通知控制器
│   └── UserOnlineStatusController.java    # 用户在线状态控制器
├── config/               # 配置类
│   ├── AsyncTaskConfig.java               # 异步任务配置
│   ├── JsonConfig.java                    # JSON配置
│   ├── MongoConfig.java                   # MongoDB配置
│   └── WebSocketDebugConfig.java          # WebSocket调试配置
├── MessageApplication.java # 启动类
└── resources/
    ├── application.yml                     # 主配置文件
    ├── application-dev.yml                 # 开发环境配置
    ├── application-local.yml               # 本地环境配置
    ├── static/                            # 静态资源
    │   ├── websocket-test.html             # WebSocket测试页面
    │   ├── stomp-test.html                 # STOMP测试页面
    │   └── debug-websocket.html            # WebSocket调试页面
    └── rabbitmq/         # RabbitMQ配置文件
        ├── exchanges.json # 交换器定义
        ├── queues.json   # 队列定义
        └── bindings.json # 绑定关系定义
```

**职责**: 应用启动、Web接口暴露、配置管理、测试页面提供等

## 🎯 RabbitMQ事件架构

### 交换器(Exchanges)设计

| 交换器 | 类型 | 描述 |
|--------|------|------|
| `device.topic` | topic | 设备事件路由(上线/下线、状态变更、告警) |
| `user.topic` | topic | 用户事件路由(登录/登出、权限变更、行为跟踪) |
| `message.topic` | topic | 消息事件路由(实时推送、状态变更) |
| `business.topic` | topic | 业务事件路由(任务、工作流、数据同步) |
| `system.topic` | topic | 系统事件路由(维护通知、公告、配置) |
| `dead.letter` | direct | 死信处理(失败消息重试) |

### 路由键(Routing Keys)规范

```
# 设备事件
device.online.{orgId}.{deviceId}     # 设备上线
device.offline.{orgId}.{deviceId}    # 设备下线
device.alert.{orgId}.{deviceId}      # 设备告警

# 用户事件  
user.login.{orgId}.{userId}          # 用户登录
user.logout.{orgId}.{userId}         # 用户登出
user.permission.{orgId}.{userId}     # 权限变更

# 消息事件
message.realtime.{orgId}.{userId}    # 实时消息推送
message.broadcast.{orgId}            # 组织广播消息

# 业务事件
business.task.{orgId}.{taskId}       # 任务事件
business.workflow.{orgId}.{processId} # 工作流事件

# 系统事件
system.maintenance.global            # 系统维护
system.announcement.{orgId}          # 组织公告
```

## 🔌 WebSocket连接协议

### 1. 原生WebSocket连接

```javascript
// 连接到原生WebSocket
const socket = new WebSocket('ws://localhost:8084/ws');

// 监听消息
socket.onmessage = function(event) {
    const message = JSON.parse(event.data);
    console.log('收到消息:', message);
};

// 发送消息
socket.send(JSON.stringify({
    type: 'USER_MESSAGE',
    content: 'Hello World',
    targetUserId: 'user123'
}));
```

### 2. STOMP协议连接

```javascript
// 连接到STOMP
const socket = new SockJS('http://localhost:8084/ws');
const stompClient = Stomp.over(socket);

// 连接并订阅
stompClient.connect({
    'CLOUD-AUTH': 'Bearer eyJhbGciOiJSUzI1NiJ9...' // Gateway认证头
}, function(frame) {
    console.log('STOMP连接成功:', frame);
    
    // 订阅个人消息
    stompClient.subscribe('/user/queue/messages', function(message) {
        console.log('个人消息:', JSON.parse(message.body));
    });
    
    // 订阅主题消息
    stompClient.subscribe('/topic/system/announcements', function(message) {
        console.log('系统公告:', JSON.parse(message.body));
    });
});

// 发送消息到指定用户
stompClient.send('/app/sendToUser', {}, JSON.stringify({
    targetUserId: 'user123',
    content: 'Hello via STOMP'
}));

// 发送广播消息
stompClient.send('/app/broadcast', {}, JSON.stringify({
    content: '系统维护通知'
}));
```

### 3. 支持的订阅端点

| 端点 | 类型 | 描述 | 示例 |
|------|------|------|------|
| `/user/queue/messages` | 个人 | 个人专属消息队列 | 私聊消息、个人通知 |
| `/user/queue/notifications` | 个人 | 个人通知队列 | 任务完成、系统提醒 |
| `/topic/system/announcements` | 主题 | 系统公告 | 维护通知、版本更新 |
| `/topic/org/{orgId}/broadcasts` | 主题 | 组织广播 | 组织内通知、会议提醒 |
| `/topic/device/{deviceId}/status` | 主题 | 设备状态 | 设备上线、故障告警 |

## 🔧 配置说明

### 应用配置

- **端口**: 8084
- **服务名**: message-service
- **原生WebSocket端点**: `/ws`
- **STOMP端点**: `/ws` (支持SockJS)
- **管理端点**: `/actuator`

### 数据库配置

```yaml
# MongoDB (消息持久化)
spring.data.mongodb:
  database: led-platform-messages-dev
  host: 192.168.1.185
  port: 27017

# Redis (在线用户状态)  
spring.redis:
  host: 192.168.1.185
  port: 6379
  database: 3  # 开发环境专用库
```

### RabbitMQ配置

```yaml
spring.rabbitmq:
  host: 192.168.1.185
  port: 5672
  username: admin
  password: admin123456
  virtual-host: /led-platform-dev
```

### WebSocket配置

```yaml
# WebSocket相关配置
websocket:
  allowed-origins: "*"  # 生产环境应限制具体域名
  buffer-size: 8192
  max-text-message-size: 32768
  max-binary-message-size: 32768
```

## 🚀 启动和部署

### 开发环境启动

```bash
# 1. 确保依赖服务运行
# - Nacos (192.168.1.185:8848)
# - MongoDB (192.168.1.185:27017)  
# - Redis (192.168.1.185:6379)
# - RabbitMQ (192.168.1.185:5672)

# 2. 启动消息服务
mvn spring-boot:run -pl message-service/message-boot

# 3. 验证服务状态
curl http://localhost:8084/actuator/health

# 4. 访问测试页面
# WebSocket测试: http://localhost:8084/websocket-test.html
# STOMP测试: http://localhost:8084/stomp-test.html
```

### Docker部署

```bash
# 构建镜像
mvn clean package -pl message-service
docker build -t message-service:latest message-service/

# 运行容器
docker run -d \
  --name message-service \
  -p 8084:8084 \
  -e SPRING_PROFILES_ACTIVE=prod \
  message-service:latest
```

## 🔗 服务集成

### Gateway集成

在Gateway中配置WebSocket代理：

```yaml
spring.cloud.gateway.routes:
  - id: message-websocket
    uri: lb://message-service
    predicates:
      - Path=/ws/**
    filters:
      - StripPrefix=0
  - id: message-rest
    uri: lb://message-service  
    predicates:
      - Path=/message/**
```

### 其他服务集成

```java
// 在core-service中发送消息事件
@Autowired
private RabbitTemplate rabbitTemplate;

// 发送用户登录事件
UserLoginEvent event = new UserLoginEvent(userId, orgId, loginTime);
rabbitTemplate.convertAndSend("user.topic", "user.login." + orgId + "." + userId, event);

// 发送设备告警事件
DeviceAlertEvent alert = new DeviceAlertEvent(deviceId, orgId, alertType, alertMessage);
rabbitTemplate.convertAndSend("device.topic", "device.alert." + orgId + "." + deviceId, alert);
```

## 📊 监控和运维

### 健康检查

- **应用健康**: `/actuator/health`
- **WebSocket状态**: `/actuator/websocket`
- **消息队列指标**: `/actuator/metrics`
- **Prometheus指标**: `/actuator/prometheus`
- **RabbitMQ健康**: `/message/rabbitmq/health`

### 关键指标监控

```yaml
# 监控指标
websocket.connections.active    # 活跃WebSocket连接数
websocket.messages.sent        # 已发送消息数
websocket.messages.received    # 已接收消息数
rabbitmq.messages.published    # RabbitMQ发布消息数
rabbitmq.messages.consumed     # RabbitMQ消费消息数
user.online.count             # 在线用户数
message.delivery.success.rate  # 消息投递成功率
```

### 日志级别

```yaml
logging.level:
  org.nan.cloud.message: DEBUG
  org.springframework.web.socket: DEBUG
  org.springframework.messaging: DEBUG
  org.springframework.amqp: INFO
```

## 🧪 测试

### 单元测试

```bash
mvn test -pl message-service
```

### 集成测试

```bash
mvn integration-test -pl message-service -Dtest.profile=integration
```

### 功能测试

#### WebSocket连接测试
访问 `http://localhost:8084/websocket-test.html` 进行原生WebSocket测试

#### STOMP协议测试  
访问 `http://localhost:8084/stomp-test.html` 进行STOMP协议测试

#### API接口测试

```bash
# 发送消息到用户
curl -X POST http://localhost:8084/message/send \
  -H "Content-Type: application/json" \
  -d '{
    "targetUserId": "user123",
    "content": "Hello World",
    "messageType": "TEXT"
  }'

# 获取用户在线状态
curl http://localhost:8084/message/user/online/status/user123

# 获取消息统计
curl http://localhost:8084/message/statistics
```

## 🚨 故障排查

### 常见问题

1. **WebSocket连接失败**
   - 检查端口是否开放
   - 验证Gateway代理配置
   - 确认认证头格式正确

2. **消息推送失败**
   - 检查RabbitMQ连接状态
   - 验证路由键配置
   - 查看消息队列堆积情况

3. **用户认证失败**
   - 检查CLOUD-AUTH头格式
   - 验证JWT令牌有效性
   - 确认Gateway认证服务正常

### 日志分析

```bash
# 查看WebSocket连接日志
grep "WebSocket" logs/web_info.log

# 查看消息投递日志
grep "MessageDelivery" logs/web_info.log

# 查看RabbitMQ相关日志
grep "RabbitMQ" logs/web_error.log
```

## 📝 后续规划

### 近期目标
1. **性能优化**: 消息批处理、连接池优化、缓存策略优化
2. **监控告警**: 集成Prometheus + Grafana仪表板
3. **安全增强**: WebSocket连接加密、消息签名验证
4. **扩展功能**: 消息模板引擎、定时消息推送

### 中期目标
1. **高可用**: 集群部署、故障转移、负载均衡
2. **消息路由**: 智能消息路由、优先级队列
3. **离线消息**: 离线消息存储和推送
4. **多协议支持**: WebRTC、长轮询等协议支持

### 长期目标
1. **大规模部署**: 支持百万级并发连接
2. **智能推送**: 基于用户行为的智能消息推送
3. **跨平台支持**: 移动端、桌面端原生支持
4. **AI集成**: 智能消息分类和内容推荐

---

**创建日期**: 2025年1月20日  
**最后更新**: 2025年7月26日  
**维护团队**: LedDeviceCloudPlatform开发组  
**版本**: v2.0.0 (新增STOMP支持)
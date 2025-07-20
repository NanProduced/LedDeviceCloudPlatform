# Message Service - 消息中心服务

## 📋 概述

Message Service 是 LedDeviceCloudPlatform 的统一消息中心服务，负责处理整个平台的实时消息推送、事件处理、WebSocket连接管理等功能。采用事件驱动架构，基于RabbitMQ消息队列，提供高可靠、高性能、可扩展的消息服务。

## 🚀 核心功能

- **🔌 WebSocket Hub**: 统一的WebSocket连接管理中心
- **📬 消息推送**: 实时消息推送给在线用户
- **🗄️ 消息持久化**: 基于MongoDB的消息存储
- **🎯 事件驱动**: 基于RabbitMQ的事件处理架构
- **👥 多租户隔离**: 支持组织级别的消息隔离
- **📊 统计分析**: 消息发送统计和性能监控
- **🔄 重试机制**: 失败消息的自动重试和死信队列处理

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
- **Spring WebSocket + STOMP** - WebSocket通信
- **RabbitMQ 3.12+** - 企业级消息队列
- **MongoDB 5.0+** - 消息持久化存储
- **Redis 6.0+** - 在线用户状态管理
- **Spring Cloud Stream** - 消息驱动微服务
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
│   ├── MessageTemplateService.java # 消息模板服务
│   ├── MessageStatisticsService.java # 消息统计服务
│   └── EventProcessorService.java  # 事件处理服务
├── domain/               # 领域模型
│   ├── Message.java      # 消息领域模型
│   ├── MessageTemplate.java # 消息模板
│   ├── MessageStatus.java   # 消息状态
│   └── EventLog.java     # 事件日志
├── repository/           # 仓储接口
├── event/                # 领域事件
│   ├── handler/          # 事件处理器
│   ├── producer/         # 事件生产者
│   └── consumer/         # 事件消费者
└── processor/            # 消息处理器
```

**职责**: 实现核心业务逻辑、领域模型定义、事件处理等

### 3. message-infrastructure - 基础设施模块

```
message-infrastructure/
├── mongodb/              # MongoDB相关
│   ├── document/         # 文档实体
│   ├── repository/       # 仓储实现
│   └── config/           # MongoDB配置
├── redis/                # Redis相关
│   ├── manager/          # Redis管理器
│   └── config/           # Redis配置
├── rabbitmq/             # RabbitMQ相关
│   ├── config/           # RabbitMQ配置
│   ├── producer/         # 消息生产者
│   ├── consumer/         # 消息消费者
│   ├── exchange/         # 交换器配置
│   └── queue/            # 队列配置
├── websocket/            # WebSocket相关
│   ├── handler/          # 消息处理器
│   ├── manager/          # 会话管理器
│   ├── router/           # 消息路由器
│   └── config/           # WebSocket配置
└── external/             # 外部系统集成
```

**职责**: 提供数据访问、外部系统集成、WebSocket实现、RabbitMQ配置等基础设施

### 4. message-boot - 启动配置模块

```
message-boot/
├── controller/           # REST控制器
├── facade/               # 应用门面
├── config/               # 配置类
├── MessageApplication.java # 启动类
└── resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-local.yml
    └── rabbitmq/         # RabbitMQ配置文件
        ├── exchanges.json # 交换器定义
        ├── queues.json   # 队列定义
        └── bindings.json # 绑定关系定义
```

**职责**: 应用启动、Web接口暴露、配置管理等

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

## 🔧 配置说明

### 应用配置

- **端口**: 8084
- **服务名**: message-service
- **WebSocket端点**: `/ws`
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
```

### 其他服务集成

```java
// 在core-service中发送消息事件
@Autowired
private RabbitTemplate rabbitTemplate;

// 发送用户登录事件
UserLoginEvent event = new UserLoginEvent(userId, orgId, loginTime);
rabbitTemplate.convertAndSend("user.topic", "user.login." + orgId + "." + userId, event);
```

## 📊 监控和运维

### 健康检查

- **应用健康**: `/actuator/health`
- **WebSocket状态**: `/actuator/websocket`
- **消息队列指标**: `/actuator/metrics`
- **Prometheus指标**: `/actuator/prometheus`

### 日志级别

```yaml
logging.level:
  org.nan.cloud.message: DEBUG
  org.springframework.web.socket: DEBUG
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

### WebSocket测试

前端连接测试：

```javascript
const socket = new SockJS('http://localhost:8084/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/user/queue/messages', function(message) {
        console.log('Received: ' + message.body);
    });
});
```

## 📝 后续规划

1. **性能优化**: 消息批处理、连接池优化
2. **监控告警**: 集成Prometheus + Grafana
3. **安全增强**: WebSocket连接认证、消息加密
4. **扩展功能**: 消息模板引擎、定时消息
5. **高可用**: 集群部署、故障转移

---

**创建日期**: 2025年1月20日  
**维护团队**: LedDeviceCloudPlatform开发组  
**版本**: v1.0.0
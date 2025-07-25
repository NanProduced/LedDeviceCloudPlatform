# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

LedDeviceCloudPlatform 是一个基于 Spring Boot 3.3.11 和 Spring Cloud 2023.0.5 构建的物联网 LED 设备控制平台。采用微服务架构，使用 OAuth2/OIDC 认证授权，实现细粒度的 RBAC 权限控制。

## 开发环境与构建

### 技术栈
- **Java 17** - 编程语言
- **Maven** - 构建工具
- **Spring Boot 3.3.11** - 基础框架
- **Spring Cloud 2023.0.5** - 微服务框架
- **Spring Cloud Alibaba 2023.0.3.3** - 阿里云组件
- **MyBatis Plus 3.5.9** - ORM框架
- **Nacos** - 服务注册与配置中心
- **Spring Security + OAuth2** - 认证授权
- **Casbin** - 权限引擎
- **Redis** - 缓存和会话存储
- **MySQL** - 主数据库
- **MongoDB** - 文档存储
- **RabbitMQ** - 消息队列

### 常用开发命令

```bash
# 构建整个项目
mvn clean install

# 构建特定模块
mvn clean install -pl gateway
mvn clean install -pl auth-server
mvn clean install -pl core-service

# 运行测试
mvn test

# 运行特定模块的测试
mvn test -pl auth-server/auth-boot

# 启动服务 (需要先启动 Nacos)
mvn spring-boot:run -pl gateway                    # 网关服务 (8082)
mvn spring-boot:run -pl auth-server/auth-boot       # 认证服务 (8081)
mvn spring-boot:run -pl core-service/core-boot      # 核心服务
mvn spring-boot:run -pl message-service/message-boot # 消息服务 (8084)
mvn spring-boot:run -pl terminal-service/terminal-boot # 终端服务

# 使用指定环境启动
mvn spring-boot:run -Dspring.profiles.active=dev
mvn spring-boot:run -Dspring.profiles.active=local

# 打包应用
mvn clean package
```

### 环境配置

项目支持多环境部署：
- **dev** - 开发环境（默认激活）
- **local** - 本地环境
- **prod** - 生产环境

配置文件位于各模块的 `src/main/resources/` 目录下：
- `application.yml` - 基础配置
- `application-dev.yml` - 开发环境配置
- `application-local.yml` - 本地环境配置

## 架构设计

### 微服务模块

1. **gateway** - API网关服务
   - 统一入口和路由管理
   - OAuth2客户端，处理认证流程
   - 基于Casbin的权限验证
   - 支持Cookie和Bearer Token认证

2. **auth-server** - 认证授权服务
   - OAuth2授权服务器
   - 支持授权码模式、PKCE模式、客户端模式
   - JWT令牌签发和验证
   - 用户身份管理

3. **core-service** - 核心业务服务
   - LED设备管理
   - 用户和组织管理
   - RBAC权限管理
   - 业务逻辑处理

4. **common-basic** - 基础通用模块
   - 异常处理
   - 工具类
   - 基础模型

5. **common-web** - Web通用模块
   - 全局异常处理
   - 响应封装
   - 请求拦截器
   - 链路追踪

6. **message-service** - 统一消息中心服务
   - 实时消息推送
   - 事件处理
   - WebSocket连接管理
   - MongoDB消息持久化

7. **terminal-service** - LED设备终端通信服务
   - 支持10K并发WebSocket连接
   - 分片式连接管理(16个分片)
   - 独立Basic Auth体系
   - HTTP轮询 + WebSocket长连接

8. **common-mq** - 通用RabbitMQ客户端模块
   - 分离式设计(core, producer, consumer)
   - 标准化的消息模型和API接口
   - 事件驱动架构支持

### 分层架构

每个业务服务遵循DDD分层架构：
- **API层** (`*-api`) - 接口定义和DTO
- **Application层** (`*-application`) - 业务逻辑和服务
- **Infrastructure层** (`*-infrastructure`) - 数据访问和外部集成
- **Boot层** (`*-boot`) - 启动配置和Web控制器

### 关键架构模式

- **Repository模式** - 数据访问抽象
- **Facade模式** - 服务门面
- **Strategy模式** - 策略实现（如OIDC用户信息映射）
- **Domain Events** - 领域事件驱动
- **CQRS** - 命令查询职责分离

## 认证授权流程

### OAuth2 + OIDC 认证
1. 用户访问受保护资源
2. Gateway重定向到Auth-Server登录页
3. 用户登录后获取授权码
4. Gateway交换Access Token和ID Token
5. 建立会话，设置Cookie
6. 后续请求携带Cookie，Gateway验证并转发

### 权限控制
- 基于Casbin的RBAC模型
- 细粒度到API级别的权限控制
- 支持多租户和组织架构
- 权限模型：Subject-Domain-URL-Action

## 消息队列架构

### RabbitMQ事件体系
- **交换器设计**: device.topic, user.topic, message.topic, business.topic, system.topic
- **路由键规范**: {event}.{action}.{orgId}.{entityId}
- **死信处理**: 支持失败消息重试和死信队列

### Common-MQ模块设计
- **分离式架构**: core, producer, consumer三层分离
- **最小依赖**: 按需引入，避免不必要的依赖
- **统一抽象**: 标准化的消息模型和API接口

## 开发规范

### 命名约定
- 类名：帕斯卡命名法 (UserController, OrderService)
- 方法和变量：驼峰命名法 (findUserById, isOrderValid)
- 常量：全大写下划线分隔 (MAX_RETRY_ATTEMPTS)
- 包名：小写，功能模块划分 (org.nan.cloud.auth.api)

### 代码风格
- 使用4个空格缩进
- 每行不超过120个字符
- 使用Egyptian风格大括号
- 始终使用中文进行交互和文档

### 异常处理
- 业务异常：可预期的业务逻辑异常
- 系统异常：不可预期的技术异常
- 使用@ControllerAdvice全局异常处理
- 保持异常链，不丢失原始异常信息

### 测试策略
- 单元测试：使用JUnit 5 + Mockito
- 集成测试：使用TestContainers
- 测试方法命名：shouldReturnUserWhenValidIdProvided
- 目标代码覆盖率：80%以上
- 测试环境：独立的application-test.yml配置，禁用外部依赖

## 配置管理

### Nacos配置
- 注册中心地址：`192.168.1.185:8848`
- 命名空间：`1e90921e-70cf-4745-87ed-dd165319ff3a`
- 支持配置热刷新

### 数据库配置
- MySQL：主数据库，用户、设备、权限等核心数据
- Redis：缓存、会话存储、在线状态管理
- MongoDB：文档存储，消息持久化、设备详情数据
- 使用HikariCP连接池

### 缓存策略
- **本地缓存**: Caffeine，支持统计和监控
- **分布式缓存**: Redis，支持TTL和键空间通知
- **多级缓存**: 本地+Redis的缓存同步机制

### 安全配置
- JWT密钥存储在`demo-jwt.jks`
- 敏感信息通过环境变量配置
- 支持HTTPS和安全头配置

## 重要工具和依赖

### 开发工具
- **Lombok** - 简化代码生成
- **MapStruct** - 对象映射
- **JRebel** - 热重载 (rebel.xml文件)
- **SpringDoc OpenAPI** - API文档生成

### 监控和日志
- **Spring Boot Actuator** - 应用监控
- **Logback** - 日志框架
- **Micrometer** - 指标收集
- **Zipkin** - 链路追踪

## 性能优化

### Terminal-Service高并发优化
- **分片式连接存储**: 16个分片，减少93.75%锁竞争
- **系统级调优**: linux-system-tuning.sh脚本
- **JVM优化**: G1GC，专门的内存和Netty参数调优
- **性能指标**: 10K并发连接，<200ms GC暂停时间

### 系统参数优化
- **网络参数**: TCP连接队列、缓冲区大小优化
- **文件句柄**: 支持65535个文件描述符
- **内存管理**: 虚拟内存和脏页参数调优

## 服务端口分配

- **Gateway**: 8082
- **Auth-Server**: 8081
- **Core-Service**: 默认随机
- **Message-Service**: 8084
- **Terminal-Service**: 与Gateway共享8082端口，通过路由区分

## 迁移计划

项目目前使用Thymeleaf模板引擎提供临时界面，计划迁移到前后端分离架构：

1. 前端接管登录界面，通过OAuth2 API完成认证
2. 前端调用REST API获取数据，不再依赖服务端模板
3. 移除Thymeleaf依赖，后端仅提供JSON API
4. 实现真正的前后端分离

## 开发流程

1. **环境准备**：启动Nacos、MySQL、Redis
2. **代码开发**：遵循DDD分层架构和编码规范
3. **测试**：编写单元测试和集成测试
4. **文档**：更新API文档和代码注释
5. **构建**：使用Maven构建和打包
6. **部署**：使用指定环境配置部署

## 故障排查

### 常见问题
- **服务启动失败**：检查Nacos连接和配置
- **认证失败**：检查JWT密钥和OAuth2配置
- **权限验证失败**：检查Casbin配置和权限模型
- **数据库连接问题**：检查连接池配置和数据库状态

### 日志级别
- ERROR：系统错误，需要立即处理
- WARN：警告信息，需要关注
- INFO：重要业务信息
- DEBUG：调试信息，开发环境使用

遵循以上指南可以高效地在LedDeviceCloudPlatform项目中进行开发和维护。
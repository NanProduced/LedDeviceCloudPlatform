# LED Device Cloud Platform (Demo)

> ⚠️ 本仓库是作者在学习阶段的演练项目，功能与生产能力都尚未完成。它记录了在微服务、统一认证、实时通信、物联网终端管理等方向的一系列技术尝试，也帮助我发现了后续需要深入优化的方向。

## 项目概览
- **目标场景**：为 LED 设备提供云端节目管理、内容分发、实时指令与状态监控能力。
- **整体架构**：Spring Boot 3 + Spring Cloud 2023 微服务，包含 Gateway、Auth、Core、Message、Terminal、File 等服务，公共能力拆分为 `common-*` 模块复用。
- **当前状态**：多数业务流程尚在设计/草稿阶段，但关键技术的 PoC 和集成打通已经完成，可作为后续项目的蓝本。

## 核心服务
| 模块 | 职责摘要 |
| --- | --- |
| `gateway` | 统一入口；充当 OAuth2/OIDC Client 与 Resource Server，集中处理登录、令牌刷新、Casbin RBAC、跨域及安全头注入。 |
| `auth-server` | 基于 Spring Authorization Server 的身份中心，自定义 OIDC Claim、Back-Channel Logout、扩展登录流程与客户端注册。 |
| `core-service` | 主要业务域 (DDD 分层)：节目/素材/组织/角色，整合 MySQL + MongoDB，提供审批、版本管理、事件发布等能力。 |
| `message-service` | STOMP WebSocket 推送与消息聚合中心，复用 `CLOUD-AUTH` 头完成网关后鉴权，支持 Topic / Queue / 用户单播。 |
| `terminal-service` | 终端指令与状态通道，计划结合 Netty/WebSocket 与 MQ；目前完成命令 ID 生成、状态事件联动等基础设施。 |
| `file-service` | 多介质文件与 VSN 播放单处理，尝试流式写入、批量缩略图、缓存策略等。 |
| `common-*` | 基础工具：二级缓存 (Caffeine + Redis)、通用异常模型、MQ 抽象、Web 上下文透传、领域对象等。 |

## 技术亮点 / 尝试
- **统一认证与 SSO**
  - Gateway 内聚授权码+PKCE、浏览器 Session、后端 Bearer Token 三种通路。
  - Auth Server 自定义 JWT Claim / OIDC 元数据 / Back-Channel Logout，支持会话级别注销与多客户端联动。

- **多层缓存与多数据源**
  - `CacheServiceImpl` 实现 L1 Caffeine + L2 Redis 双层缓存，支持批量、ZSet、统计与回源策略。
  - 节目内容采用 MySQL + MongoDB 混合存储，利用事件驱动触发 VSN 转码并生成审批记录。

- **消息与实时通信**
  - 通用 MQ Message 模型封装路由键、重试、过期、优先级等元数据，便于对接 RabbitMQ / RocketMQ。
  - STOMP WebSocket + SockJS + Gateway 鉴权链路贯通，终端状态通过 MQ 投递至消息服务再推送至前端。

- **领域驱动设计实践**
  - 服务 `*-api / *-application / *-infrastructure / *-boot` 分层，Controller 只调用 Facade，Domain 保持纯净。
  - 结合 Facade + AOP 处理租户、身份、审批流程，探索在多租户业务下的代码组织方式。

- **终端侧置换与命令体系**
  - 自定义高并发指令 ID 生成器 (时间戳 + 计数 + 偏移)。
  - 计划引入 WebSocket 分片、Netty 通道池、命令确认队列，目前完成 MQ 形态的演练。

## 学到的经验
1. **认证链路复杂度不可低估**：仅在 Gateway 层剥离/再包装身份头会引入零信任威胁，需要在后续项目中改善为端到端签名或 mTLS。
2. **跨存储事务要提前设计幂等与补偿**：节目流程中涉及 MySQL、MongoDB、消息队列，必须引入事务消息或 Saga 模式。
3. **实时推送与终端通信要统一协议模型**：`CLOUD-AUTH`、MQ、WebSocket 的数据模型需要进一步抽象，避免信息重复解析。
4. **DDD 分层能提升可维护性，但要配套自动化测试与脚手架**，否则代码量激增时维护成本高。

## 后续计划
- 重新梳理身份链路：保留标准 Bearer Token 验证，或给下游内网流量增加签名/密钥。
- 引入消息事务、分布式锁、批处理工具，完善节目/素材等关键路径的可靠性。
- 构建统一的实时通信网关，将 STOMP、Netty、MQ等能力规范化并复用到其他项目。
- 深化对 Casbin、Spring Authorization Server、Caffeine/Redis 双层缓存、流式大文件处理等技术的研究，并提炼成独立组件。

## 快速启动 (Demo)
> Demo 环境依赖 MySQL、Redis、MongoDB、RabbitMQ、Nacos，目前配置写死在 `application-local.yml` 中，仅用于本地演示。

```bash
# 主工程打包
mvn clean package -DskipTests

# 分服务示例启动（需按需修改配置）
cd auth-server/auth-boot
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 结语
虽然这个项目因时间/目标调整暂时搁置，但它沉淀了大量有价值的探索记录。后续我会在新的场景中继续打磨这些能力，让它们真正服务于生产环境。

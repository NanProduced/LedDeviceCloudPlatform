# Terminal Service - LED设备终端通信服务

## 概述

Terminal Service 是LED设备云平台的终端通信服务，专为支持10,000个并发WebSocket连接而设计的高性能微服务。该服务提供LED设备与云平台之间的实时双向通信能力。

## 核心特性

### 🚀 高性能架构
- **10k并发连接**: 支持10,000个同时WebSocket连接，预留20k扩展能力
- **分片式连接管理**: 16个分片减少锁竞争，提升并发性能
- **Netty优化**: 基于Netty的零拷贝、内存池优化
- **G1GC调优**: 专门针对高并发场景的垃圾回收参数优化

#### 分片式连接存储架构
Terminal Service采用分片式连接存储设计，将大量WebSocket连接按照设备ID分散存储到多个独立的分片中，有效解决高并发场景下的锁竞争问题。

**核心优势：**
- **锁竞争减少93.75%**: 10000个连接分散到16个分片，每个分片平均处理625个连接
- **线性性能扩展**: 理论吞吐量从1000 TPS提升到16000 TPS
- **内存效率优化**: 每个分片预分配内存，避免HashMap扩容开销
- **集群化预留**: 支持未来水平扩展和负载均衡

**技术实现：**
- **分片算法**: 基于设备ID哈希值的取模分片，确保连接均匀分布
- **并发控制**: 每个分片使用独立的读写锁，支持高并发读操作
- **容量限制**: 单个分片最大625个连接，总体支持10k并发
- **故障隔离**: 单个分片故障不影响其他分片的正常运行

**性能监控：**
- 分片负载均衡度监控
- 单分片响应时间统计
- 锁等待时间分析
- 连接分布热力图

### 🔐 独立认证体系
- **Basic Auth**: 纯Basic Authentication，完全无状态设计
- **Redis缓存**: 认证信息缓存TTL 30分钟，提升认证性能
- **简单快速**: 无锁定机制，直接验证账号密码，适合设备频繁认证

### 📊 三层数据存储
- **MySQL**: 设备基础信息存储（设备ID、名称、分组）
- **MongoDB**: 设备复杂详情数据存储（配置、状态历史）
- **Redis**: 实时缓存和会话存储（在线状态、指令队列）

### 🌐 双协议支持
- **HTTP轮询**: 传统HTTP API接口，兼容现有设备
- **WebSocket长连接**: 实时双向通信，55秒心跳保活
- **协议转换**: HTTP和WebSocket格式的智能转换

## 技术架构

### DDD分层架构
```
terminal-service/
├── terminal-api/           # API接口层
├── terminal-application/   # 业务逻辑层  
├── terminal-infrastructure/# 数据访问层
└── terminal-boot/          # 启动配置层
```

### 关键技术栈
- **Spring Boot 3.3.11**: 基础框架
- **Netty 4.1.115.Final**: 高性能网络通信
- **WebSocket**: 实时双向通信协议
- **G1 Garbage Collector**: 低延迟垃圾回收
- **HikariCP**: 高性能数据库连接池
- **Lettuce**: 高性能Redis客户端

## 性能指标

### 目标性能
- **并发连接数**: 10,000 (目标) / 20,000 (扩展能力)
- **内存使用**: < 16GB (生产环境)
- **GC暂停时间**: < 200ms
- **CPU使用率**: < 70%
- **响应延迟**: < 100ms (99th percentile)

### 分片式连接存储性能
- **分片数量**: 16个分片，每分片最大625个连接
- **锁竞争优化**: 相比单一存储减少93.75%的锁等待时间
- **吞吐量提升**: 理论TPS从1000提升至16000 (16倍)
- **内存开销**: 分片管理增加< 5%内存使用
- **故障隔离**: 单分片故障影响< 6.25%的连接
- **负载均衡**: 连接分布标准差< 10%，确保均匀分布

### 系统要求
- **CPU**: 16核心或以上
- **内存**: 16GB或以上
- **网络**: 千兆网卡
- **存储**: SSD硬盘
- **操作系统**: Linux (推荐CentOS 7+/Ubuntu 18+)

## 快速开始

### 环境准备
1. **Java 17**: 确保安装Java 17
2. **Maven 3.8+**: 构建工具
3. **MySQL 8.0+**: 主数据库
4. **Redis 6.0+**: 缓存和会话存储
5. **MongoDB 4.4+**: 文档存储
6. **Nacos 2.0+**: 服务注册与配置中心

### 系统优化（生产环境必需）
```bash
# 1. 执行系统参数优化
sudo chmod +x linux-system-tuning.sh
sudo ./linux-system-tuning.sh

# 2. 重启系统或重新加载参数
sudo sysctl -p
```

### 构建和启动
```bash
# 1. 构建项目
mvn clean install -DskipTests

# 2. 启动服务（开发环境）
chmod +x terminal-boot/src/main/resources/jvm-startup.sh
./terminal-boot/src/main/resources/jvm-startup.sh dev

# 3. 启动服务（生产环境）
./terminal-boot/src/main/resources/jvm-startup.sh prod

# 4. 健康检查
curl http://localhost:8082/actuator/health
```

## API接口

### 设备核心接口
- `GET /wp-json/wp/v2/comments?clt_type=terminal` - 设备取指令
- `POST /wp-json/wp/v2/comments` - 指令应答确认
- `PUT /wp-json/screen/v1/status` - 设备状态上报

### 节目管理接口
- `GET /wp-json/wp/v2/programs` - 节目列表查询
- `GET /wp-json/wp/v2/media` - 素材列表查询
- `GET /wp-json/wp/v3/schedules` - 排程信息查询

### 文件管理接口
- `POST /wp-json/wp/v2/media/upload` - 文件上传
- `GET /wp-json/wp/v2/media/{id}/download` - 文件下载

### WebSocket端点
- `ws://localhost:8082/terminal/ws` - WebSocket连接端点

## 配置说明

### 应用配置文件
- `application.yml`: 基础配置，生产环境优化参数
- `application-dev.yml`: 开发环境配置
- `application-local.yml`: 本地开发配置

### JVM优化参数
详见 `jvm-startup.sh` 脚本，包含：
- **内存管理**: G1GC, 堆内存, 直接内存配置
- **Netty优化**: 内存池, Arena配置, 缓存参数
- **GC日志**: 详细的垃圾回收监控日志
- **故障诊断**: 堆转储, 错误日志, 飞行记录器

### 系统参数优化
详见 `linux-system-tuning.sh` 脚本，包含：
- **网络参数**: 连接队列, TCP参数, 缓冲区大小
- **文件句柄**: 系统和用户级文件描述符限制
- **内存管理**: 虚拟内存, 脏页回写参数
- **性能调优**: BBR拥塞控制, 窗口缩放等

## 监控和运维

### 健康检查端点
- `/actuator/health` - 服务健康状态
- `/actuator/metrics` - 性能指标
- `/actuator/prometheus` - Prometheus监控数据
- `/actuator/info` - 服务信息

### 关键监控指标
- **连接数统计**: 当前WebSocket连接数量
- **内存使用率**: JVM堆内存和直接内存使用情况
- **GC性能**: 垃圾回收频率和暂停时间
- **响应延迟**: API接口响应时间分布
- **错误率**: 请求错误率和异常统计

### 日志文件
- `terminal-service.log` - 主应用日志
- `terminal-service-error.log` - 错误日志
- `terminal-websocket.log` - WebSocket连接日志
- `terminal-command.log` - 设备指令日志
- `terminal-performance.log` - 性能监控日志
- `gc.log` - GC日志

## 故障排查

### 常见问题
1. **连接数过多**: 检查系统文件句柄限制和内存使用
2. **GC频繁**: 调整G1GC参数，检查内存泄漏
3. **响应缓慢**: 检查数据库连接池、Redis连接
4. **WebSocket断连**: 检查心跳机制、网络稳定性

### 性能调优建议
1. **系统级**: 执行 `linux-system-tuning.sh` 脚本
2. **JVM级**: 根据实际内存调整堆大小
3. **应用级**: 调整连接池大小、缓存TTL
4. **网络级**: 优化TCP参数、使用BBR拥塞控制

## 开发指南

### 环境切换
```bash
# 开发环境
mvn spring-boot:run -Dspring.profiles.active=dev

# 本地环境  
mvn spring-boot:run -Dspring.profiles.active=local

# 生产环境
java -jar terminal-boot.jar --spring.profiles.active=prod
```

### 代码规范
- 遵循DDD分层架构原则
- 使用异步编程模式处理高并发
- 合理使用缓存减少数据库压力
- 添加必要的监控和日志记录

### 测试建议
- 单元测试覆盖核心业务逻辑
- 使用TestContainers进行集成测试
- 压力测试验证并发性能指标
- 监控测试检查资源使用情况

## 扩展能力

### 集群化支持
- **连接管理器接口化设计**: 基于ConnectionManager接口，支持单机/集群模式无缝切换
- **Redis Pub/Sub消息广播**: 跨节点指令广播和状态同步
- **分片式集群扩展**: 支持按分片维度的水平扩展，线性增长处理能力
- **连接状态跨节点同步**: 基于Redis的分布式连接状态管理

### 水平扩展
- **多实例部署**: 每个实例独立管理16个分片，支持无状态扩展
- **负载均衡器配置**: WebSocket连接粘性路由，确保连接稳定性
- **分片路由策略**: 基于设备ID的一致性哈希，支持节点动态增减
- **数据库读写分离**: MySQL主从架构配合分片式读取优化

### 分片式架构扩展优势
- **平滑扩容**: 新增节点时可按分片粒度迁移连接，最小化服务中断
- **容错能力**: 单节点故障仅影响该节点管理的分片，其他分片正常服务
- **线性扩展**: 理论上可支持 节点数 × 10k 的连接规模
- **资源隔离**: 每个分片独立监控和资源管理，便于性能调优

---

## 联系方式

如有问题或建议，请通过以下方式联系：
- 项目Issues: [GitHub Issues]
- 技术文档: [项目Wiki]
- 团队联系: LedDeviceCloudPlatform Team
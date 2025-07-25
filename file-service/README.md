# File Service - 文件服务模块

## 📋 概述

File Service 是 LedDeviceCloudPlatform 的核心文件处理服务，专为LED设备内容管理平台设计。基于DDD（领域驱动设计）分层架构，提供企业级的文件上传、视频转码、存储管理等核心功能，支撑LED显示内容的全生命周期管理。

## 🎯 设计目标

- **🎬 多媒体处理**: 支持视频、图片、音频等多种格式的上传和转码
- **📱 LED内容优化**: 针对LED显示屏特点进行内容格式优化
- **🚀 高性能处理**: 支持大文件上传、GPU加速转码、并发处理
- **☁️ 云存储集成**: 无缝集成本地存储和阿里云OSS
- **⚡ 实时监控**: WebSocket实时推送转码进度和处理状态
- **🔒 安全可靠**: 完整的权限控制、文件验证和审计日志

## 🚀 核心功能

### 📤 文件上传服务
- **单文件上传**: 支持常见文件格式的快速上传
- **批量上传**: 并发处理多个文件，提高上传效率
- **分块上传**: 支持大文件(5GB+)的断点续传功能
- **秒传功能**: 基于MD5哈希的文件去重，避免重复上传
- **进度跟踪**: 实时监控上传进度，支持暂停和恢复
- **格式验证**: 自动检测文件格式和MIME类型
- **缩略图生成**: 自动为图片和视频生成多尺寸预览图

### 🎬 视频转码服务
- **FFmpeg引擎**: 集成业界标准的FFmpeg转码引擎
- **GPU加速**: 支持NVIDIA GPU硬件加速转码
- **多格式输出**: 支持H.264/H.265/VP9等主流编码格式
- **预设配置**: 内置LED显示优化的转码预设
- **实时进度**: WebSocket推送转码进度和性能指标
- **批量转码**: 支持队列化的批量视频处理
- **质量优化**: 智能码率控制和画质优化
- **失败重试**: 自动重试机制和错误恢复

### 💾 存储管理服务
- **多存储支持**: 本地存储 + 阿里云OSS双重支持
- **智能选择**: 根据文件大小和类型自动选择存储策略
- **版本管理**: 完整的文件版本控制和历史回滚
- **生命周期**: 自动化的文件生命周期管理
- **空间监控**: 实时监控存储空间使用情况
- **数据同步**: 本地和云端存储的智能同步

### 📊 文件管理API
- **查询检索**: 支持关键词、标签、类型等多维度搜索
- **批量操作**: 批量删除、移动、复制、重命名文件
- **权限控制**: 基于组织和用户的细粒度访问控制
- **统计报告**: 存储使用量、转码统计等数据分析
- **文件预览**: 支持图片、视频、文档的在线预览
- **下载管理**: 安全的文件下载和访问控制

## 🏗️ 架构设计

### DDD分层架构

```
file-service/
├── file-api/          # API定义层 - 接口契约和数据传输对象
├── file-application/  # 应用服务层 - 业务逻辑和流程编排  
├── file-infrastructure/ # 基础设施层 - 外部系统集成和技术实现
└── file-boot/         # 启动配置层 - Web端点和应用配置
```

### 技术栈

- **Spring Boot 3.3.11** - 基础框架
- **Spring Cloud 2023.0.5** - 微服务框架
- **FFmpeg 4.4+** - 视频转码引擎
- **Alibaba Cloud OSS** - 云存储服务
- **Spring WebSocket** - 实时进度推送
- **MyBatis Plus 3.5.9** - 数据访问框架
- **Redis 6.0+** - 缓存和进度存储
- **MongoDB** - 文件元数据存储
- **RabbitMQ** - 异步任务队列

## 📂 模块结构详解

### 1. file-api - API定义模块 ✅

```
file-api/
├── FileUploadApi.java         # 文件上传API接口
├── TranscodingApi.java        # 视频转码API接口  
├── FileManagementApi.java     # 文件管理API接口
└── dto/                       # 数据传输对象(47个DTO类)
    ├── FileUploadRequest.java     # 文件上传请求
    ├── FileUploadResponse.java    # 文件上传响应
    ├── TranscodingTaskRequest.java # 转码任务请求
    ├── TranscodingProgressResponse.java # 转码进度响应
    ├── ChunkUpload*.java          # 分块上传相关DTO
    ├── BatchOperation*.java       # 批量操作相关DTO
    ├── FileStatistics*.java       # 统计分析相关DTO
    └── ...                        # 其他DTO类
```

**当前状态**: ✅ **已完成** - 3个主要API接口和47个DTO类全部实现

**职责**: 
- 定义对外REST API接口契约
- 数据传输对象和参数验证
- Swagger API文档注解
- 统一响应格式封装

### 2. file-application - 应用服务层 ⚠️

```
file-application/
├── service/                   # 业务服务接口
│   ├── FileUploadService.java      # 文件上传服务接口
│   ├── TranscodingService.java     # 转码服务接口
│   ├── StorageService.java         # 存储服务接口 [待实现]
│   ├── FileValidationService.java  # 文件验证服务接口 [待实现]
│   ├── ProgressTrackingService.java # 进度跟踪服务接口 [待实现]
│   ├── ThumbnailService.java       # 缩略图服务接口 [待实现]
│   └── impl/                       # 服务实现类
│       ├── FileUploadServiceImpl.java # 文件上传服务实现 [框架完成]
│       └── TranscodingServiceImpl.java # 转码服务实现 [待实现]
├── domain/                    # 领域模型 [待实现]
│   ├── FileInfo.java              # 文件信息聚合根
│   ├── TranscodingTask.java       # 转码任务实体
│   └── StorageLocation.java       # 存储位置值对象
└── repository/                # 仓储接口 [待实现]
    ├── FileInfoRepository.java    # 文件信息仓储
    └── TranscodingTaskRepository.java # 转码任务仓储
```

**当前状态**: ⚠️ **框架完成** - 服务接口定义，实现类需完善

**待完成任务**:
- [ ] 实现4个核心依赖服务接口
- [ ] 创建FileInfo等领域对象
- [ ] 创建Repository接口定义
- [ ] 完成TranscodingServiceImpl实现

### 3. file-infrastructure - 基础设施层 ⚡

```
file-infrastructure/
├── transcoding/               # 转码引擎 ✅
│   └── FFmpegTranscoder.java       # FFmpeg转码器实现
├── progress/                  # 进度管理 ✅  
│   ├── TranscodingProgressWebSocketHandler.java # WebSocket进度处理器
│   └── StompProgressNotifier.java # STOMP进度通知器
├── storage/                   # 存储实现 [待实现]
│   ├── LocalStorageProvider.java   # 本地存储提供者
│   ├── OssStorageProvider.java     # OSS存储提供者
│   └── StorageStrategy.java        # 存储策略
├── repository/                # 数据访问 [待实现]
│   ├── FileInfoRepositoryImpl.java # 文件信息仓储实现
│   └── mybatis/                    # MyBatis映射配置
└── config/                    # 基础设施配置 [待实现]
    ├── StorageConfig.java          # 存储配置
    ├── TranscodingConfig.java      # 转码配置
    └── CacheConfig.java            # 缓存配置
```

**当前状态**: ⚡ **部分完成** - 转码引擎和进度推送已实现

**已完成组件**:
- ✅ FFmpeg转码引擎 - 完整的视频转码功能
- ✅ WebSocket进度推送 - 实时转码进度监控
- ✅ STOMP进度通知 - 集成消息服务推送

**待完成任务**:
- [ ] 存储服务提供者实现
- [ ] 数据访问层Repository实现
- [ ] 缓存和配置管理组件

### 4. file-boot - 启动配置层 🔧

```
file-boot/
├── FileServiceApplication.java # 应用启动类 ✅
├── config/                     # 配置类 [部分完成]
│   ├── WebSocketConfig.java         # WebSocket配置 ✅
│   ├── StorageConfig.java           # 存储配置 [待实现]
│   ├── TranscodingConfig.java       # 转码配置 [待实现]
│   └── AsyncConfig.java             # 异步任务配置 [待实现]
├── controller/                 # REST控制器 [待实现]
│   ├── FileUploadController.java    # 文件上传控制器
│   ├── TranscodingController.java   # 转码控制器
│   └── FileManagementController.java # 文件管理控制器
└── resources/                  # 配置资源
    ├── application.yml              # 主配置文件 ✅
    ├── application-dev.yml          # 开发环境配置 ✅
    └── static/                      # 静态资源
        └── transcoding-progress.html # 转码进度监控页面 ✅
```

**当前状态**: 🔧 **基础完成** - 启动类和基本配置已实现

**待完成任务**:
- [ ] 创建3个主要REST控制器
- [ ] 完善配置类和数据库配置
- [ ] 添加异步任务和缓存配置

## 🎬 转码引擎特性

### FFmpeg集成特性

```java
// GPU加速转码配置
TranscodingConfig config = TranscodingConfig.builder()
    .enableGpuAcceleration(true)
    .videoCodec("h264_nvenc")  // NVIDIA GPU编码
    .resolution("1920x1080")
    .bitrate(2000)
    .frameRate(30)
    .build();

// 实时进度回调
FFmpegTranscoder transcoder = new FFmpegTranscoder();
TranscodingResult result = transcoder.transcode(
    inputPath, outputPath, config,
    progress -> {
        // 实时进度回调，通过WebSocket推送
        progressHandler.broadcastProgress(taskId, progress);
    }
);
```

### 支持的转码格式

| 输入格式 | 输出格式 | 编码器 | 特点 |
|---------|---------|-------|------|
| MP4, AVI, MOV | MP4 | H.264/H.265 | 高兼容性，适合LED播放 |
| MKV, WEBM | WEBM | VP8/VP9 | 开源格式，质量优秀 |
| 各种格式 | HLS | H.264 | 流媒体格式，支持自适应码率 |

### 转码预设配置

| 预设名 | 分辨率 | 码率 | 帧率 | 适用场景 |
|--------|--------|------|------|---------|
| LED_HD | 1920x1080 | 2000kbps | 30fps | 高清LED大屏 |
| LED_FHD | 1920x1080 | 4000kbps | 60fps | 超高清LED显示 |
| LED_4K | 3840x2160 | 8000kbps | 30fps | 4K LED显示屏 |
| WEB_PREVIEW | 1280x720 | 1000kbps | 25fps | Web预览播放 |

## 🔌 实时进度监控

### WebSocket进度推送

```javascript
// 连接转码进度WebSocket
const socket = new WebSocket('ws://localhost:8085/file/progress');

// 订阅转码任务进度
const subscribeMessage = {
    action: 'SUBSCRIBE',
    taskId: 'transcode_12345'
};
socket.send(JSON.stringify(subscribeMessage));

// 接收实时进度更新
socket.onmessage = function(event) {
    const progressData = JSON.parse(event.data);
    if (progressData.type === 'PROGRESS') {
        updateProgressBar(progressData.data.progress);
        updateProcessingInfo(progressData.data);
    }
};
```

### 进度数据格式

```json
{
    "type": "PROGRESS",
    "taskId": "transcode_12345",
    "data": {
        "progress": 75,
        "status": "PROCESSING",
        "fps": 30.5,
        "processedDuration": 180,
        "totalDuration": 240,
        "estimatedTimeRemaining": 60,
        "currentTime": "00:03:00",
        "frameCount": 5400,
        "outputFileSize": 25165824,
        "bitrate": 2048,
        "qualityScore": 0.95
    },
    "timestamp": 1703568000000
}
```

## 📊 API接口概览

### 文件上传API

| 端点 | 方法 | 描述 | 示例 |
|------|------|------|------|
| `/file/upload/single` | POST | 单文件上传 | 上传单个视频文件 |
| `/file/upload/batch` | POST | 批量文件上传 | 同时上传多个图片 |
| `/file/upload/chunk/init` | POST | 初始化分块上传 | 大文件分块上传准备 |
| `/file/upload/chunk/{chunkNumber}` | POST | 上传文件分块 | 上传第N个文件块 |
| `/file/upload/chunk/complete` | POST | 完成分块上传 | 合并所有分块 |
| `/file/upload/progress/{uploadId}` | GET | 查询上传进度 | 获取上传状态 |

### 视频转码API

| 端点 | 方法 | 描述 | 示例 |
|------|------|------|------|
| `/file/transcoding/submit` | POST | 提交转码任务 | 转码视频为LED格式 |
| `/file/transcoding/batch` | POST | 批量转码任务 | 批量处理多个视频 |
| `/file/transcoding/progress/{taskId}` | GET | 查询转码进度 | 获取转码状态 |
| `/file/transcoding/cancel/{taskId}` | DELETE | 取消转码任务 | 停止转码处理 |
| `/file/transcoding/presets` | GET | 获取转码预设 | 查看可用预设配置 |
| `/file/transcoding/queue/status` | GET | 查询队列状态 | 查看转码队列情况 |

### 文件管理API

| 端点 | 方法 | 描述 | 示例 |
|------|------|------|------|
| `/file/management/info/{fileId}` | GET | 获取文件信息 | 查看文件详情 |
| `/file/management/list` | GET | 文件列表查询 | 分页查询文件 |
| `/file/management/search` | POST | 文件搜索 | 关键词搜索文件 |
| `/file/management/download/{fileId}` | GET | 下载文件 | 下载原始文件 |
| `/file/management/preview/{fileId}` | GET | 文件预览 | 获取预览URL |
| `/file/management/batch` | POST | 批量操作 | 批量删除/移动文件 |

## 🔧 配置说明

### 应用配置

- **服务端口**: 8085
- **服务名**: file-service
- **WebSocket端点**: `/file/progress`
- **管理端点**: `/actuator`

### 存储配置

```yaml
# 存储策略配置
file:
  storage:
    strategy: AUTO  # AUTO, LOCAL, OSS
    local:
      base-path: /data/files
      temp-path: /data/temp
    oss:
      endpoint: https://oss-cn-hangzhou.aliyuncs.com
      bucket: led-platform-files
      access-key: ${OSS_ACCESS_KEY}
      secret-key: ${OSS_SECRET_KEY}
```

### 转码配置

```yaml
# 转码引擎配置  
file:
  transcoding:
    ffmpeg-path: /usr/local/bin/ffmpeg
    temp-directory: /data/temp/transcoding
    max-concurrent-tasks: 3
    enable-gpu: true
    gpu-device: 0
    default-preset: LED_HD
```

### 上传限制配置

```yaml
# 文件上传配置
spring:
  servlet:
    multipart:
      max-file-size: 5GB
      max-request-size: 5GB
      
file:
  upload:
    chunk-size: 5MB
    max-chunks: 1000
    allowed-types:
      - video/mp4
      - video/avi
      - image/jpeg
      - image/png
```

## 🚀 快速开始

### 环境准备

```bash
# 1. 安装FFmpeg (带GPU支持)
sudo apt update
sudo apt install ffmpeg nvidia-cuda-toolkit

# 2. 配置阿里云OSS (可选)
export OSS_ACCESS_KEY="your-access-key"
export OSS_SECRET_KEY="your-secret-key"

# 3. 启动依赖服务
# - Nacos (192.168.1.185:8848)
# - Redis (192.168.1.185:6379)
# - MySQL (192.168.1.185:3306)
```

### 启动服务

```bash
# 开发环境启动
mvn spring-boot:run -pl file-service/file-boot

# 生产环境启动
mvn clean package -pl file-service
java -jar file-service/file-boot/target/file-boot-1.0-SNAPSHOT.jar

# Docker启动
docker run -d \
  --name file-service \
  -p 8085:8085 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /data/files:/data/files \
  file-service:latest
```

### 验证服务

```bash
# 健康检查
curl http://localhost:8085/actuator/health

# 访问转码进度监控页面
open http://localhost:8085/transcoding-progress.html

# 测试文件上传API (需要先实现控制器)
curl -X POST http://localhost:8085/file/upload/single \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test-video.mp4" \
  -F "organizationId=org123"
```

## 📊 监控和运维

### 健康检查端点

- **应用健康**: `/actuator/health`
- **转码队列状态**: `/actuator/metrics`
- **存储空间监控**: `/file/management/storage/status`
- **系统性能**: `/actuator/prometheus`

### 关键性能指标

```yaml
# 监控指标
file.upload.active.count      # 活跃上传任务数
file.upload.success.rate      # 上传成功率
transcoding.queue.size        # 转码队列长度
transcoding.processing.count  # 正在处理的转码任务
storage.usage.local          # 本地存储使用量
storage.usage.oss            # OSS存储使用量
ffmpeg.gpu.utilization       # GPU使用率
websocket.progress.connections # 进度监控连接数
```

### 日志配置

```yaml
logging.level:
  org.nan.cloud.file: DEBUG
  org.nan.cloud.file.infrastructure.transcoding: INFO
  org.nan.cloud.file.infrastructure.progress: DEBUG
```

## 🧪 测试和调试

### 功能测试

#### 转码进度监控测试
访问 `http://localhost:8085/transcoding-progress.html` 进行WebSocket转码进度测试

#### API接口测试

```bash
# 测试文件上传 (需要先实现控制器)
curl -X POST http://localhost:8085/file/upload/single \
  -F "file=@sample-video.mp4" \
  -F "organizationId=test-org"

# 测试转码任务提交
curl -X POST http://localhost:8085/file/transcoding/submit \
  -H "Content-Type: application/json" \
  -d '{
    "fileId": "file123",
    "preset": "LED_HD",
    "outputFormat": "mp4",
    "organizationId": "test-org"
  }'

# 查询转码进度
curl http://localhost:8085/file/transcoding/progress/task123
```

### 性能测试

```bash
# 并发上传测试
for i in {1..10}; do
  curl -X POST http://localhost:8085/file/upload/single \
    -F "file=@test-file-$i.mp4" &
done

# 转码性能测试
curl -X POST http://localhost:8085/file/transcoding/batch \
  -H "Content-Type: application/json" \
  -d '{
    "tasks": [
      {"fileId": "file1", "preset": "LED_HD"},
      {"fileId": "file2", "preset": "LED_FHD"}
    ]
  }'
```

## 📋 待完成任务清单

### 🔥 高优先级 (运行必需)

1. **核心服务实现**
   - [ ] StorageService - 存储服务实现
   - [ ] FileValidationService - 文件验证服务
   - [ ] ProgressTrackingService - 进度跟踪服务
   - [ ] ThumbnailService - 缩略图生成服务

2. **数据访问层**
   - [ ] FileInfo领域对象创建
   - [ ] FileInfoRepository接口和实现
   - [ ] 数据库表结构设计

3. **Web控制器层**
   - [ ] FileUploadController实现
   - [ ] TranscodingController实现  
   - [ ] FileManagementController实现

### ⚡ 中优先级 (功能完善)

4. **业务服务完善**
   - [ ] TranscodingServiceImpl完整实现
   - [ ] 异步任务配置和队列管理
   - [ ] 文件生命周期管理

5. **配置和集成**
   - [ ] 数据库连接池配置
   - [ ] Redis缓存配置
   - [ ] OSS存储集成配置

### 📝 低优先级 (质量提升)

6. **测试和文档**
   - [ ] 单元测试编写
   - [ ] 集成测试覆盖
   - [ ] API文档完善

7. **性能优化**
   - [ ] 大文件处理优化
   - [ ] 转码队列优化
   - [ ] 缓存策略优化

## 🚨 常见问题排查

### 转码相关问题

1. **FFmpeg未找到**
   ```bash
   # 检查FFmpeg安装
   which ffmpeg
   ffmpeg -version
   
   # 安装FFmpeg
   sudo apt install ffmpeg
   ```

2. **GPU加速失败**
   ```bash
   # 检查NVIDIA驱动
   nvidia-smi
   
   # 检查CUDA支持
   ffmpeg -hwaccels
   ```

3. **转码任务失败**
   ```bash
   # 查看转码日志
   grep "FFmpeg" logs/file-service.log
   
   # 检查临时目录权限
   ls -la /data/temp/transcoding
   ```

### 存储相关问题

1. **本地存储空间不足**
   ```bash
   # 检查磁盘空间
   df -h /data/files
   
   # 清理临时文件
   find /data/temp -type f -mtime +1 -delete
   ```

2. **OSS连接失败**
   ```bash
   # 检查网络连接
   ping oss-cn-hangzhou.aliyuncs.com
   
   # 验证访问密钥
   curl -H "Authorization: OSS your-access-key:signature" \
        https://your-bucket.oss-cn-hangzhou.aliyuncs.com/
   ```

## 📝 版本规划

### v1.1.0 (当前开发版)
- [ ] 完成核心功能实现
- [ ] 基础REST API上线  
- [ ] 单机版本转码支持

### v1.2.0 (计划中)
- [ ] 集群部署支持
- [ ] 高可用转码队列
- [ ] 智能存储策略

### v2.0.0 (远期目标)
- [ ] AI辅助内容优化
- [ ] 跨地域存储同步
- [ ] 大规模并发处理

---

**创建日期**: 2025年7月26日  
**维护团队**: LedDeviceCloudPlatform开发组  
**版本**: v1.0.0 (初始版本)  
**状态**: 🚧 开发中 - API层完成，实现层待完善
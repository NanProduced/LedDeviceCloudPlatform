# Message-Service WebSocket API 前后端对接文档

## 📋 文档概述

本文档整理了`message-service`中所有`CommonStompMessage`的构造模式，提供标准JSON示例供前后端对接使用。

**项目**: LedDeviceCloudPlatform  
**服务**: message-service  
**协议**: STOMP over WebSocket  
**消息格式**: CommonStompMessage统一封装

---

## 🎯 消息类型概览

| 消息类型 | 业务场景 | 实现状态 | 优先级 |
|---------|---------|---------|-------|
| COMMAND_FEEDBACK | LED指令执行反馈 | ✅ 已实现 | HIGH |
| TERMINAL_STATUS | 终端状态变更通知 | ✅ 已实现 | NORMAL |
| TASK_PROGRESS | 任务进度更新 | ✅ 已实现 | NORMAL |
| TOPIC_SUBSCRIBE_FEEDBACK | 订阅反馈 | ✅ 已实现 | NORMAL |

---

## 📨 消息类型详细说明

### 1. COMMAND_FEEDBACK - LED指令执行反馈

#### 1.1 单设备指令反馈

**业务场景**: LED设备执行单个控制指令后的结果反馈

**Java构造方法**: `CommandMessageProcessor.buildCommandResultMessage()`

**JSON示例**:
```json
{
  "messageId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "timestamp": "2025-08-11T08:30:45.123Z",
  "oid": 10001,
  "messageType": "COMMAND_FEEDBACK",
  "subType_1": "SINGLE",
  "subType_2": "SUCCESS",
  "level": null,
  "context": {
    "resourceType": "COMMAND", 
    "uid": 20001,
    "tid": 30001,
    "commandId": "cmd_led_brightness_001"
  },
  "payload": {
    "commandType": "SET_BRIGHTNESS",
    "deviceId": 30001,
    "parameters": {
      "brightness": 80
    },
    "executionTime": "2025-08-11T08:30:44.856Z",
    "result": "设备亮度已调整到80%"
  },
  "priority": "HIGH",
  "requireAck": null,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**字段说明**:
- `subType_1`: 固定为"SINGLE"，表示单设备指令
- `subType_2`: 执行结果，SUCCESS/FAILED/REJECT
- `context.commandId`: 指令唯一标识符
- `payload`: 原始指令内容和执行结果
- `priority`: 固定HIGH，指令反馈需要立即处理

**前端处理**: 根据subType_2显示成功/失败状态，payload中包含详细执行信息

---

#### 1.2 批量指令进度反馈

**业务场景**: 批量LED设备指令执行进度

**Java构造方法**: `CommandMessageProcessor.buildBatchProgressMessage()` 

**⚠️ 注意**: 当前实现返回null，功能缺失

**期望JSON示例**:
```json
{
  "messageId": "b58cc10f-47ac-4372-a567-0e02b2c3d480",
  "timestamp": "2025-08-11T08:32:15.456Z", 
  "oid": 10001,
  "messageType": "TASK_PROGRESS",
  "subType_1": "BATCH_COMMAND",
  "subType_2": "PROGRESS",
  "level": "INFO",
  "context": {
    "resourceType": "BATCH",
    "uid": 20001,
    "tid": null,
    "batchId": "batch_cmd_001",
    "commandId": "cmd_led_brightness_batch"
  },
  "title": "批量指令执行进度",
  "content": "批量设备指令执行进度: 15/20 完成",
  "payload": {
    "batchId": "batch_cmd_001",
    "totalCount": 20,
    "completedCount": 15,
    "successCount": 13,
    "failureCount": 2,
    "progress": 75,
    "currentDevice": "LED-DISPLAY-001"
  },
  "priority": "NORMAL",
  "requireAck": false,
  "ttl": 180000,
  "actions": [
    {
      "actionId": "view-batch-detail",
      "actionName": "查看详情",
      "actionType": "VIEW",
      "actionTarget": "/dashboard/commands/batch/batch_cmd_001"
    }
  ]
}
```

---

### 2. TERMINAL_STATUS - 终端状态变更通知

#### 2.1 终端在线状态变更

**业务场景**: LED终端设备上线/离线状态变化通知

**Java构造方法**: `TerminalStatusMessageProcessor.processTerminalOnlineStatusChange()`

**JSON示例**:
```json
{
  "messageId": "c3d47910-58cc-4372-a567-0e02b2c3d481",
  "timestamp": "2025-08-11T08:35:20.789Z",
  "oid": 10001,
  "messageType": "TERMINAL_STATUS",
  "subType_1": "ONLINE",
  "level": null,
  "context": {
    "resourceType": "TERMINAL",
    "tid": 30001
  },
  "payload": {
    "timestamp": "2025-08-11T08:35:20.123Z"
  },
  "priority": "NORMAL",
  "requireAck": null,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**字段说明**:
- `subType_1`: ONLINE/OFFLINE，表示终端状态
- `context.tid`: 终端设备ID
- `payload.timestamp`: 状态变更时间

**前端处理**: 更新设备列表中对应设备的在线状态显示

---

#### 2.2 LED状态数据上报

**业务场景**: LED终端设备状态数据主动上报

**Java构造方法**: `TerminalStatusMessageProcessor.processLedStatusReport()`

**JSON示例**:
```json
{
  "messageId": "d481c3d4-58cc-4372-a567-0e02b2c3d482",
  "timestamp": "2025-08-11T08:40:30.123Z",
  "oid": 10001,
  "messageType": "TERMINAL_STATUS",
  "subType_1": "LED_STATUS",
  "level": null,
  "context": {
    "resourceType": "TERMINAL",
    "tid": 30001
  },
  "payload": {
    "report": {
      "deviceId": "LED-DISPLAY-001",
      "brightness": 85,
      "temperature": 42.5,
      "voltage": 12.3,
      "playingContent": "advertisement_video_001.mp4",
      "cpuUsage": 25.6,
      "memoryUsage": 68.2,
      "storageUsage": 45.8,
      "networkStatus": "CONNECTED",
      "lastHeartbeat": "2025-08-11T08:40:29.856Z"
    }
  },
  "priority": "NORMAL",
  "requireAck": null,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**字段说明**:
- `subType_1`: 固定为"LED_STATUS"
- `payload.report`: LED设备详细状态数据

**前端处理**: 更新设备监控面板的实时状态数据

---

### 3. TASK_PROGRESS - 任务进度更新

#### 3.1 文件上传进度

**业务场景**: 素材文件上传进度实时更新

**Java构造方法**: `FileUploadMessageProcessor.createUploadProgressMessage()`

**JSON示例**:
```json
{
  "messageId": "file_upload_1691736645123",
  "timestamp": "2025-08-11T08:45:15.456Z",
  "oid": 10001,
  "messageType": "TASK_PROGRESS",
  "level": "INFO",
  "title": "文件上传进度",
  "content": "文件 advertisement_video.mp4 上传进度: 65%",
  "context": {
    "resourceType": "FILE",
    "uid": 20001,
    "taskId": "upload_task_001"
  },
  "payload": {
    "eventType": "PROGRESS",
    "taskId": "upload_task_001",
    "fileName": "advertisement_video.mp4",
    "progress": 65,
    "uploadedBytes": 67108864,
    "totalBytes": 103546880,
    "timestamp": "2025-08-11T08:45:15.456Z"
  },
  "priority": null,
  "requireAck": null,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**前端处理**: 更新上传进度条，显示百分比和传输速度

---

#### 3.2 文件上传完成

**业务场景**: 素材文件上传完成通知

**Java构造方法**: `FileUploadMessageProcessor.createUploadCompletedMessage()`

**JSON示例**:
```json
{
  "messageId": "file_upload_1691736680789",
  "timestamp": "2025-08-11T08:48:30.789Z", 
  "oid": 10001,
  "messageType": "TASK_PROGRESS",
  "level": "SUCCESS",
  "title": "文件上传完成",
  "content": "文件 advertisement_video.mp4 上传并处理完成",
  "context": {
    "resourceType": "FILE",
    "uid": 20001,
    "taskId": "upload_task_001"
  },
  "payload": {
    "eventType": "COMPLETED",
    "taskId": "upload_task_001",
    "fileId": "file_67890",
    "fileName": "advertisement_video.mp4",
    "timestamp": "2025-08-11T08:48:30.789Z"
  },
  "priority": null,
  "requireAck": null,
  "ttl": null,
  "actions": [
    {
      "actionId": "view-file",
      "actionName": "查看文件",
      "actionType": "VIEW",
      "actionTarget": "/dashboard/materials/file/file_67890"
    }
  ],
  "extra": null
}
```

**前端处理**: 显示成功通知，提供文件查看链接

---

#### 3.3 文件上传失败

**业务场景**: 素材文件上传失败通知

**Java构造方法**: `FileUploadMessageProcessor.createUploadFailedMessage()`

**JSON示例**:
```json
{
  "messageId": "file_upload_1691736700123",
  "timestamp": "2025-08-11T08:50:00.123Z",
  "oid": 10001,
  "messageType": "TASK_PROGRESS", 
  "level": "ERROR",
  "title": "文件上传失败",
  "content": "文件 large_video.mp4 上传失败: 文件大小超过限制",
  "context": {
    "resourceType": "FILE",
    "uid": 20001,
    "taskId": "upload_task_002"
  },
  "payload": {
    "eventType": "FAILED",
    "taskId": "upload_task_002",
    "fileName": "large_video.mp4",
    "errorCode": "FILE_SIZE_EXCEEDED",
    "errorMessage": "文件大小超过限制",
    "timestamp": "2025-08-11T08:50:00.123Z"
  },
  "priority": null,
  "requireAck": null,
  "ttl": null,
  "actions": [
    {
      "actionId": "retry-upload",
      "actionName": "重新上传",
      "actionType": "RETRY",
      "actionTarget": "/api/files/retry/upload_task_002"
    }
  ],
  "extra": null
}
```

**前端处理**: 显示错误通知，提供重试操作

---

#### 3.4 批量文件上传进度

**业务场景**: 批量文件上传的聚合进度

**Java构造方法**: `FileUploadMessageProcessor.createBatchUploadProgressMessage()`

**JSON示例**:
```json
{
  "messageId": "file_upload_1691736720456",
  "timestamp": "2025-08-11T08:52:30.456Z",
  "oid": 10001, 
  "messageType": "TASK_PROGRESS",
  "level": "INFO",
  "title": "批量文件上传进度",
  "content": "批量上传进度: 75.5% (15/20) - 当前文件: promo_image_15.jpg",
  "context": {
    "resourceType": "FILE",
    "uid": 20001,
    "taskId": "batch_upload_001"
  },
  "payload": {
    "eventType": "PROGRESS",
    "batchId": "batch_upload_001",
    "taskId": "batch_upload_001",
    "currentFileName": "promo_image_15.jpg",
    "totalFiles": 20,
    "completedFiles": 15,
    "failedFiles": 1,
    "overallProgress": 75.5,
    "totalUploadedBytes": 157286400,
    "totalBytes": 208896000,
    "batchProgressDetails": [
      {
        "taskId": "sub_task_001",
        "filename": "promo_image_15.jpg",
        "progress": 100.0,
        "uploadedBytes": 2048000,
        "totalBytes": 2048000,
        "status": "COMPLETED"
      }
    ],
    "timestamp": "2025-08-11T08:52:30.456Z"
  },
  "priority": null,
  "requireAck": null,
  "ttl": null,
  "actions": [
    {
      "actionId": "view-batch-detail",
      "actionName": "查看批量任务详情", 
      "actionType": "VIEW",
      "actionTarget": "/dashboard/files/batch/batch_upload_001"
    }
  ],
  "extra": null
}
```

**前端处理**: 显示批量上传的总体进度和单个文件详情

---

### 4. TOPIC_SUBSCRIBE_FEEDBACK - 订阅反馈

#### 4.1 主题订阅成功反馈

**业务场景**: 用户订阅WebSocket主题后的确认反馈

**Java构造方法**: `SubscriptionManager.sendSubscriptionFeedback()`

**JSON示例**:
```json
{
  "messageId": "3d479c58-cc10-4372-a567-0e02b2c3d483",
  "timestamp": "2025-08-11T08:55:10.789Z",
  "oid": 10001,
  "messageType": "TOPIC_SUBSCRIBE_FEEDBACK", 
  "level": "IGNORE",
  "context": null,
  "payload": {
    "operation": "SUBSCRIBE",
    "topic": "/topic/terminal/30001",
    "success": true,
    "message": "订阅主题成功",
    "subscriptionCount": 3,
    "timestamp": "2025-08-11T08:55:10.789Z"
  },
  "priority": "NORMAL",
  "requireAck": false,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**字段说明**:
- `level`: 固定为"IGNORE"，前端通常不显示此类消息
- `payload.operation`: SUBSCRIBE/UNSUBSCRIBE
- `payload.topic`: 订阅的主题路径

**前端处理**: 更新订阅状态，通常不向用户显示

---

#### 4.2 取消订阅反馈

**业务场景**: 用户取消订阅WebSocket主题后的确认反馈

**Java构造方法**: `SubscriptionManager.sendUnsubscriptionFeedback()`

**JSON示例**:
```json
{
  "messageId": "4e583d47-9c58-4372-a567-0e02b2c3d484",
  "timestamp": "2025-08-11T08:58:20.123Z",
  "oid": 10001,
  "messageType": "TOPIC_SUBSCRIBE_FEEDBACK",
  "level": "IGNORE", 
  "context": null,
  "payload": {
    "operation": "UNSUBSCRIBE",
    "topic": "/topic/terminal/30001",
    "success": true,
    "message": "取消订阅成功",
    "subscriptionCount": 2,
    "timestamp": "2025-08-11T08:58:20.123Z"
  },
  "priority": "NORMAL",
  "requireAck": false,
  "ttl": null,
  "actions": null,
  "extra": null
}
```

**前端处理**: 更新订阅状态，清理相关UI组件

---

## 🔧 实现问题和建议

### ❌ 当前缺陷

1. **批量指令进度功能缺失**
   - 位置: `CommandMessageProcessor.buildBatchProgressMessage():278`
   - 问题: 方法返回null，批量指令进度完全不可用
   - 影响: 无法向前端推送批量LED控制指令的执行进度

2. **时间戳格式不统一**
   - 问题: 混用`Instant.now().toString()`和`LocalDateTime.now().toString()`
   - 建议: 统一使用ISO8601格式 `Instant.now().toString()`

3. **MessageAction使用率低**
   - 问题: 大部分消息没有提供操作按钮
   - 建议: 为关键消息添加VIEW、RETRY等操作选项

### ✅ 优化建议

1. **统一消息构造器**
   ```java
   // 建议封装
   public class StompMessageBuilder {
       public static CommandFeedbackBuilder command() { ... }
       public static TaskProgressBuilder task() { ... }
       public static TerminalStatusBuilder terminal() { ... }
   }
   ```

2. **扩展操作类型**
   - 添加LED设备特有操作：PAUSE, RESUME, STOP, RESTART, CONFIG
   - 完善现有操作的参数传递

3. **消息模板化**
   - 为常见场景提供标准模板
   - 支持多语言内容生成

---

## 📋 前端集成检查清单

### 必须验证的字段
- [x] `messageId`: 全局唯一性检查
- [x] `timestamp`: ISO8601格式解析  
- [x] `oid`: 组织权限验证
- [x] `messageType`: 枚举值有效性
- [x] `level`: UI展示样式映射

### 推荐处理逻辑
- [x] 消息去重：基于messageId
- [x] 过期检查：基于ttl字段
- [x] 权限验证：基于oid和context.uid
- [x] 优先级处理：基于priority字段
- [x] 操作按钮：基于actions数组

### 错误处理策略
- [x] 格式错误：记录日志，丢弃消息
- [x] 权限错误：静默丢弃，避免信息泄露  
- [x] 业务错误：显示友好提示

---

**文档版本**: v1.0  
**生成时间**: 2025-08-11  
**维护人**: Backend Team
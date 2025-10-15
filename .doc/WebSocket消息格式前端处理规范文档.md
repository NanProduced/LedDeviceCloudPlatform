# WebSocket消息格式前端处理规范文档

## 📋 文档概述

本文档面向后端开发人员，详细说明前端对WebSocket消息的处理逻辑和要求。请严格按照本规范发送消息，确保前端能够正确处理和展示。

**适用范围**：云平台WebSocket实时通信  
**消息协议**：STOMP over WebSocket  
**消息格式**：统一Message结构

---

---

## ⚡ 前端支持的操作类型 (actionType)

### 1. DOWNLOAD - 下载操作
```json
{
  "actionId": "download-file",
  "actionName": "下载文件",
  "actionType": "DOWNLOAD",
  "actionTarget": "/api/files/download/file-123456",
  "parameters": {
    "fileId": "file-123456",
    "fileName": "report.pdf"
  }
}
```
**前端处理**：直接触发浏览器下载

### 2. VIEW - 查看详情
```json
{
  "actionId": "view-task",
  "actionName": "查看任务详情",
  "actionType": "VIEW",
  "actionTarget": "/dashboard/program-management/task/task-123456",
  "parameters": {
    "taskId": "task-123456"
  }
}
```
**前端处理**：使用Next.js路由跳转到详情页

### 3. NAVIGATE - 页面导航
```json
{
  "actionId": "goto-device",
  "actionName": "查看设备",
  "actionType": "NAVIGATE", 
  "actionTarget": "/dashboard/devices",
  "parameters": {
    "deviceId": "dev-789012",
    "highlight": true
  }
}
```
**前端处理**：路由跳转并高亮显示指定设备

### 4. CONFIRM - 确认操作
```json
{
  "actionId": "confirm-delete",
  "actionName": "确认删除",
  "actionType": "CONFIRM",
  "actionTarget": "/api/tasks/delete/task-123456",
  "parameters": {
    "taskId": "task-123456",
    "confirmText": "确定要删除这个任务吗？"
  }
}
```
**前端处理**：显示确认对话框，用户确认后调用API

### 5. RETRY - 重试操作
```json
{
  "actionId": "retry-transcode",
  "actionName": "重新转码",
  "actionType": "RETRY",
  "actionTarget": "/api/transcode/retry/trans-901234",
  "parameters": {
    "transcodeId": "trans-901234"
  }
}
```
**前端处理**：调用重试API并显示处理结果

### 6. REFRESH - 刷新数据
```json
{
  "actionId": "refresh-list",
  "actionName": "刷新列表",
  "actionType": "REFRESH",
  "actionTarget": "/api/tasks/list",
  "parameters": {
    "scope": "task-list",
    "filters": {}
  }
}
```
**前端处理**：刷新指定范围的数据

---

## 🎨 字段处理规则

### level字段 - UI展示方式
| level | 前端处理 | 展示效果 | 自动消失时间 |
|-------|---------|---------|------------|
| SUCCESS | 绿色Toast通知 | 成功图标 + 绿色背景 | 3秒 |
| INFO | 蓝色Toast通知 | 信息图标 + 蓝色背景 | 5秒 |
| WARNING | 黄色Alert组件 | 警告图标 + 黄色背景 | 手动关闭 |
| ERROR | 红色Modal弹窗 | 错误图标 + 红色背景 | 必须确认 |

### priority字段 - 处理优先级
| priority | 前端处理 | 说明 |
|----------|---------|------|
| HIGH | 立即显示，可能伴随声音提示 | 紧急消息 |
| NORMAL | 正常队列处理 | 普通消息 |
| LOW | 延迟显示，可批量处理 | 非重要消息 |

### requireAck字段 - 确认机制
- `true`：必须显示Modal确认框，用户确认后前端发送ACK到 `/app/message/ack/{messageId}`
- `false`：自动处理，无需用户交互

### ttl字段 - 过期时间
- **建议范围**：60000ms - 3600000ms (1分钟 - 1小时)
- **前端处理**：消息过期后自动从UI中清理
- **特殊值**：0 表示永不过期（谨慎使用）

---

## 🔄 消息处理流程

```mermaid
flowchart TD
    A[接收WebSocket消息] --> B{验证messageId}
    B -->|重复| C[丢弃消息]
    B -->|新消息| D{检查ttl}
    D -->|已过期| C
    D -->|未过期| E[根据messageType分发]
    E --> F[根据level决定UI展示]
    F --> G{requireAck?}
    G -->|true| H[显示确认框]
    G -->|false| I[自动处理]
    H --> J[用户确认后发送ACK]
    I --> K[渲染actions按钮]
    J --> K
    K --> L[更新相关页面状态]
```

---

## ⚠️ 后端开发注意事项

### 必填字段验证
```json
{
  "messageId": "必须全局唯一，建议使用UUID",
  "timestamp": "必须是ISO8601格式：2025-01-01T10:30:45.123Z",
  "oid": "必须与用户组织ID匹配",
  "messageType": "必须是前端支持的枚举值",
  "level": "必须是 SUCCESS/INFO/WARNING/ERROR 之一"
}
```

### 发送频率控制
- **建议频率**：同一用户每秒不超过10条消息
- **批量消息**：使用 `aggregate: true` 合并相似消息
- **进度消息**：建议每2-5秒发送一次，避免过于频繁

### 权限检查
- 确保消息只发送给有权限的用户
- 组织消息只发送给对应组织的用户
- 敏感信息不要通过WebSocket传输

### 错误处理
- 发送失败时记录日志
- 提供消息重发机制
- 监控消息发送成功率

---

## 🧪 测试建议

### 测试用例覆盖
1. **正常消息**：各种messageType的标准格式
2. **边界情况**：最大payload、特殊字符、空值处理
3. **异常情况**：格式错误、缺失字段、过期消息
4. **性能测试**：高频消息、大量并发用户

### 示例测试数据
```json
{
  "messageId": "test-msg-001",
  "timestamp": "2025-01-01T10:30:45.123Z",
  "oid": 10001,
  "messageType": "TASK_PROGRESS",
  "subType_1": "TRANSCODE",
  "level": "INFO",
  "context": {
    "resourceType": "TASK",
    "uid": 20002,
    "taskId": "task-test-001"
  },
  "title": "转码任务进度更新",
  "content": "您的视频转码进度已更新至75%",
  "payload": {
    "taskId": "task-test-001",
    "progress": 75,
    "status": "PROCESSING",
    "stage": "转码中"
  },
  "priority": "NORMAL",
  "requireAck": false,
  "ttl": 300000,
  "actions": [
    {
      "actionId": "view-task",
      "actionName": "查看详情",
      "actionType": "VIEW",
      "actionTarget": "/dashboard/program-management/task/task-test-001"
    }
  ]
}
```

---

## 🔄 版本兼容性

### 向后兼容原则
- ✅ 新增可选字段
- ✅ 新增messageType（需提前通知）
- ✅ 新增actionType（需提前通知）
- ❌ 删除现有必填字段
- ❌ 修改现有字段类型
- ❌ 修改现有枚举值

### 变更通知流程
1. 提前1周通知前端团队
2. 提供详细的变更说明
3. 提供测试环境验证
4. 确认前端兼容后再上线

---

## ❓ 常见问题FAQ

### Q1: 消息发送后前端没有反应？
**A**: 检查以下项目：
- messageId是否重复
- messageType是否在支持列表中
- oid是否与用户组织匹配
- 消息格式是否正确

### Q2: 进度消息应该多久发送一次？
**A**: 建议2-5秒发送一次，避免过于频繁影响前端性能

### Q3: 如何处理长时间运行的任务？
**A**: 使用合适的ttl值，定期发送进度更新，完成时发送最终状态

### Q4: actions按钮点击后没有响应？
**A**: 检查actionTarget是否是有效的API地址或路由

### Q5: 如何调试WebSocket消息？
**A**: 前端提供了STOMP测试工具，可以在 `/dashboard/stomp-test` 页面进行调试

---

**文档版本**: v1.0  
**最后更新**: 2025-01-01  
**联系人**: 前端开发团队
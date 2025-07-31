package org.nan.cloud.message.infrastructure.websocket.stomp.enums;

/**
 * STOMP Topic和Destination统一管理 - 极简版
 * 
 * 设计理念：
 * - 消息分类职责归于CommonStompMessage：通过messageType、subType等字段区分消息类型
 * - 前端统一处理：订阅少量主题，通过消息内容路由到具体处理逻辑
 * 
 * 核心主题：
 * 1. /user/queue/messages - 所有个人消息统一队列
 * 2. /topic/org/{orgId} - 组织级广播消息
 * 3. /topic/device/{deviceId} - 设备相关消息
 * 4. /topic/task/{taskId} - 任务相关消息  
 * 5. /topic/system - 系统级消息
 * 6. /topic/batch/{batchId} - 批量处理相关消息
 * 
 * @author Nan
 * @since 2.0.0 - 极简重构版
 */
public final class StompTopic {

    private StompTopic() {
        // 工具类，禁止实例化
    }

    // ==================== 核心主题定义 ====================
    
    /**
     * 个人消息统一队列
     * 包括：通知、任务进度、指令反馈、订阅结果等所有个人消息
     */
    public static final String USER_MESSAGES_QUEUE = "/queue/messages";

    /**
     * 组织消息主题模板
     * 包括：公告、告警、组织级状态变更等所有组织消息
     * 实际使用: /topic/org/{orgId}
     */
    public static final String ORG_TOPIC_TEMPLATE = "/topic/org/{orgId}";

    /**
     * 设备消息主题模板  
     * 包括：状态变更、数据流、指令执行等所有设备消息
     * 实际使用: /topic/device/{deviceId}
     */
    public static final String DEVICE_TOPIC_TEMPLATE = "/topic/device/{deviceId}";

    /**
     * 任务消息主题模板
     * 包括：任务状态、执行结果等所有任务消息
     * 实际使用: /topic/task/{taskId}
     */
    public static final String TASK_TOPIC_TEMPLATE = "/topic/task/{taskId}";

    /**
     * 批量操作聚合消息主题模板
     * 包括：批量指令聚合
     * 实际使用: /topic/batch/{batchId}
     */
    public static final String BATCH_AGG_TOPIC_TEMPLATE = "/topic/batch/{batchId}";

    /**
     * 系统消息主题
     * 包括：系统公告、维护通知、紧急警报等所有系统级消息
     */
    public static final String SYSTEM_TOPIC = "/topic/system";

    // ==================== 工具方法 ====================
    
    /**
     * 构建组织主题路径
     * 
     * @param orgId 组织ID
     * @return 组织主题路径 /topic/org/{orgId}
     */
    public static String buildOrgTopic(String orgId) {
        return ORG_TOPIC_TEMPLATE.replace("{orgId}", orgId);
    }
    
    /**
     * 构建设备主题路径
     * 
     * @param deviceId 设备ID (可以是终端ID、设备组ID等)
     * @return 设备主题路径 /topic/device/{deviceId}
     */
    public static String buildDeviceTopic(String deviceId) {
        return DEVICE_TOPIC_TEMPLATE.replace("{deviceId}", deviceId);
    }
    
    /**
     * 构建任务主题路径
     * 
     * @param taskId 任务ID
     * @return 任务主题路径 /topic/task/{taskId}
     */
    public static String buildTaskTopic(String taskId) {
        return TASK_TOPIC_TEMPLATE.replace("{taskId}", taskId);
    }

    /**
     * 构建批量聚合消息主题路径
     *
     * @param batchId 批量操作Id
     * @return 任务主题路径 /topic/task/{taskId}
     */
    public static String buildBatchAggTopic(String batchId) {
        return BATCH_AGG_TOPIC_TEMPLATE.replace("{batchId}", batchId);
    }
    
}

/*
 * ==================== 极简化设计说明 ====================
 * 
 * 前端订阅示例：
 * 
 * ```javascript
 * stompClient.onConnect = (frame) => {
 *     // 1. 订阅个人消息（所有类型统一）
 *     stompClient.subscribe('/user/queue/messages', (message) => {
 *         const msg = JSON.parse(message.body);
 *         
 *         // 通过消息类型路由
 *         switch (msg.messageType) {
 *             case 'NOTIFICATION':
 *                 handleNotification(msg);
 *                 break;
 *             case 'COMMAND_FEEDBACK': 
 *                 handleCommandFeedback(msg);
 *                 break;
 *             case 'TOPIC_SUBSCRIBE_FEEDBACK':
 *                 handleSubscribeFeedback(msg);
 *                 break;
 *             case 'TASK_PROGRESS':
 *                 handleTaskProgress(msg);
 *                 break;
 *         }
 *     });
 *     
 *     // 2. 订阅组织消息（手动）
 *     client.subscribe('/topic/org/' + orgId, handleOrgMessage);
 *     
 *     // 3. 订阅系统消息（手动）  
 *     client.subscribe('/topic/system', handleSystemMessage);
 *     
 *     // 4. 按需订阅设备消息
 *     subscribeToDevice('device123');
 *     
 *     // 5. 按需订阅任务消息
 *     subscribeToTask('task456');
 * };
 * 
 * function subscribeToDevice(deviceId) {
 *     stompClient.subscribe(`/topic/device/${deviceId}`, (message) => {
 *         const msg = JSON.parse(message.body);
 *         
 *         // 通过subType区分设备消息类型
 *         if (msg.subType_1 === 'STATUS_CHANGE') {
 *             handleDeviceStatus(msg);
 *         } else if (msg.subType_1 === 'DATA_STREAM') {
 *             handleDeviceData(msg);
 *         } else if (msg.subType_1 === 'COMMAND_EXECUTION') {
 *             handleDeviceCommand(msg);
 *         }
 *     });
 * }
 * ```
 * 
 * 消息类型映射：
 * 
 * | 旧设计 | 新设计 | CommonStompMessage字段 |
 * |--------|--------|----------------------|
 * | /topic/user/{userId}/notifications | /user/queue/messages | messageType=NOTIFICATION |
 * | /topic/user/{userId}/tasks/progress | /user/queue/messages | messageType=TASK_PROGRESS |
 * | /topic/user/{userId}/commands/feedback | /user/queue/messages | messageType=COMMAND_FEEDBACK |
 * | /queue/subscribe-result | /user/queue/messages | messageType=TOPIC_SUBSCRIBE_FEEDBACK |
 * | /topic/org/{orgId}/announcements | /topic/org/{orgId} | messageType=NOTIFICATION, subType_1=ANNOUNCEMENT |
 * | /topic/terminal/{id}/status | /topic/device/{id} | messageType=DEVICE_STATUS, subType_1=STATUS_CHANGE |
 * | /topic/terminal/{id}/data/streaming | /topic/device/{id} | messageType=DEVICE_DATA, subType_1=DATA_STREAM |
 * | /topic/commandTask/{id}/progress | /topic/task/{id} | messageType=TASK_PROGRESS, subType_1=BATCH_COMMAND |
 * | /topic/global/system/announcements | /topic/system | messageType=SYSTEM_NOTIFICATION |
 * 
 * 优势：
 * 1. 主题数量从30+减少到5个模式
 * 2. 前端订阅逻辑大幅简化
 * 3. 权限控制更清晰（按资源类型而不是具体消息类型）
 * 4. 消息分类职责明确（CommonStompMessage负责）
 * 5. 易于扩展新的消息类型
 */

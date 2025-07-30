package org.nan.cloud.message.infrastructure.websocket.stomp.enums;

/**
 * STOMP Topic和Destination统一管理
 * 
 * 用于统一管理所有STOMP相关的主题路径和目标路径，
 * 便于集中维护和避免硬编码。
 * 
 * 命名规范：
 * - Topic: 多个客户端可以订阅的广播主题，使用/topic前缀
 * - Queue: 点对点消息队列，使用/queue前缀  
 * - App: 客户端发送消息的应用端点，使用/app前缀
 * 
 * @author Nan
 * @since 1.0.0
 */
public final class StompTopic {
    
    private StompTopic() {
        // 工具类，禁止实例化
    }
    
    // ==================== 用户级别 Topic ====================
    
    /**
     * 用户个人通知主题模板
     * 实际使用: /topic/user/{userId}/notifications
     */
    public static final String USER_NOTIFICATION_TOPIC_TEMPLATE = "/topic/user/{userId}/notifications";
    
    /**
     * 用户任务进度主题模板  
     * 实际使用: /topic/user/{userId}/tasks/progress
     */
    public static final String USER_TASK_PROGRESS_TOPIC_TEMPLATE = "/topic/user/{userId}/tasks/progress";
    
    /**
     * 用户指令反馈主题模板
     * 实际使用: /topic/user/{userId}/commands/feedback
     */
    public static final String USER_COMMAND_FEEDBACK_TOPIC_TEMPLATE = "/topic/user/{userId}/commands/feedback";
    
    // ==================== 组织级别 Topic ====================
    
    /**
     * 组织公告主题模板
     * 实际使用: /topic/org/{orgId}/announcements
     */
    public static final String ORG_ANNOUNCEMENT_TOPIC_TEMPLATE = "/topic/org/{orgId}/announcements";
    
    /**
     * 组织终端状态聚合主题模板
     * 实际使用: /topic/org/{orgId}/terminals/status/aggregated
     */
    public static final String ORG_TERMINAL_STATUS_TOPIC_TEMPLATE = "/topic/org/{orgId}/terminals/status/aggregated";
    
    /**
     * 组织监控数据主题模板
     * 实际使用: /topic/org/{orgId}/monitoring/dashboard
     */
    public static final String ORG_MONITORING_TOPIC_TEMPLATE = "/topic/org/{orgId}/monitoring/dashboard";
    
    // ==================== 终端级别 Topic ====================
    
    /**
     * 终端状态主题模板
     * 不仅仅是在线、离线状态的变更。还包括终端上报数据中的其他状态变更
     * 实际使用: /topic/terminal/{terminalId}/status
     */
    public static final String TERMINAL_STATUS_TOPIC_TEMPLATE = "/topic/terminal/{terminalId}/status";
    
    /**
     * 终端指令执行状态主题模板
     * 实际使用: /topic/terminal/{terminalId}/commands/execution
     */
    public static final String TERMINAL_COMMAND_TOPIC_TEMPLATE = "/topic/terminal/{terminalId}/commands/execution";
    
    /**
     * 终端数据流主题模板
     * 实际使用: /topic/terminal/{terminalId}/data/streaming
     */
    public static final String TERMINAL_DATA_TOPIC_TEMPLATE = "/topic/terminal/{terminalId}/data/streaming";
    
    // ==================== 批量指令 Topic ====================
    
    /**
     * 批量指令进度摘要主题模板
     * 实际使用: /topic/commandTask/{taskId}/progress/summary
     */
    public static final String BATCH_COMMAND_SUMMARY_TOPIC_TEMPLATE = "/topic/commandTask/{taskId}/progress/summary";
    
    /**
     * 批量指令详细进度主题模板
     * 实际使用: /topic/commandTask/{taskId}/progress/detailed
     */
    public static final String BATCH_COMMAND_DETAILED_TOPIC_TEMPLATE = "/topic/commandTask/{taskId}/progress/detailed";
    
    /**
     * 批量指令最终结果主题模板
     * 实际使用: /topic/commandTask/{taskId}/result/final
     */
    public static final String BATCH_COMMAND_RESULT_TOPIC_TEMPLATE = "/topic/commandTask/{taskId}/result/final";
    
    // ==================== 终端组级别 Topic ====================
    
    /**
     * 终端组批量指令主题模板
     * 实际使用: /topic/terminalGroup/{tgId}/commands/batch
     */
    public static final String TERMINAL_GROUP_BATCH_COMMAND_TOPIC_TEMPLATE = "/topic/terminalGroup/{tgId}/commands/batch";
    
    /**
     * 终端组状态变更主题模板
     * 实际使用: /topic/terminalGroup/{tgId}/status/changes
     */
    public static final String TERMINAL_GROUP_STATUS_TOPIC_TEMPLATE = "/topic/terminalGroup/{tgId}/status/changes";
    
    /**
     * 终端组实时监控主题模板
     * 实际使用: /topic/terminalGroup/{tgId}/monitoring/realtime
     */
    public static final String TERMINAL_GROUP_MONITORING_TOPIC_TEMPLATE = "/topic/terminalGroup/{tgId}/monitoring/realtime";
    
    // ==================== 聚合消息 Topic ====================
    
    /**
     * 用户批量指令聚合主题模板
     * 实际使用: /topic/aggregated/batch-commands/{userId}
     */
    public static final String AGGREGATED_BATCH_COMMANDS_TOPIC_TEMPLATE = "/topic/aggregated/batch-commands/{userId}";
    
    /**
     * 终端组聚合状态主题模板
     * 实际使用: /topic/aggregated/terminal-groups/{tgIds}
     */
    public static final String AGGREGATED_TERMINAL_GROUPS_TOPIC_TEMPLATE = "/topic/aggregated/terminal-groups/{tgIds}";
    
    /**
     * 监控数据聚合主题模板
     * 实际使用: /topic/aggregated/monitoring/{dashboardId}
     */
    public static final String AGGREGATED_MONITORING_TOPIC_TEMPLATE = "/topic/aggregated/monitoring/{dashboardId}";
    
    // ==================== 全局系统 Topic ====================
    
    /**
     * 系统公告主题
     */
    public static final String GLOBAL_SYSTEM_ANNOUNCEMENT_TOPIC = "/topic/global/system/announcements";
    
    /**
     * 维护通知主题
     */
    public static final String GLOBAL_MAINTENANCE_TOPIC = "/topic/global/maintenance/notifications";
    
    /**
     * 紧急警报主题
     */
    public static final String GLOBAL_EMERGENCY_TOPIC = "/topic/global/emergency/alerts";
    
    // ==================== 用户队列 Destination ====================
    
    /**
     * 用户通知消息队列
     */
    public static final String USER_NOTIFICATION_DESTINATION = "/queue/notifications";
    
    /**
     * 用户消息结果队列
     */
    public static final String USER_MESSAGE_RESULT_DESTINATION = "/queue/message-result";
    
    /**
     * 用户心跳响应队列
     */
    public static final String USER_PONG_DESTINATION = "/queue/pong";
    
    /**
     * 用户订阅结果队列
     */
    public static final String USER_SUBSCRIBE_RESULT_DESTINATION = "/queue/subscribe-result";
    
    /**
     * 用户错误消息队列
     */
    public static final String USER_ERROR_DESTINATION = "/queue/error";
    
    /**
     * 用户自动订阅建议队列
     */
    public static final String USER_AUTO_SUBSCRIBE_DESTINATION = "/queue/auto-subscribe";
    
    /**
     * 用户欢迎消息队列
     */
    public static final String USER_WELCOME_DESTINATION = "/queue/welcome";
    
    /**
     * 批量指令结果队列
     */
    public static final String USER_BATCH_COMMAND_RESULT_DESTINATION = "/queue/batch-command-result";
    
    // ==================== 应用端点 App Destination ====================
    
    /**
     * 终端指令应用端点模板
     * 实际使用: /app/terminal/command/{terminalId}
     */
    public static final String APP_TERMINAL_COMMAND_TEMPLATE = "/app/terminal/command/{terminalId}";
    
    /**
     * 用户消息应用端点
     */
    public static final String APP_USER_MESSAGE = "/app/user/message";
    
    /**
     * 系统心跳应用端点
     */
    public static final String APP_SYSTEM_PING = "/app/system/ping";
    
    /**
     * 终端订阅应用端点模板
     * 实际使用: /app/terminal/subscribe/{terminalId}
     */
    public static final String APP_TERMINAL_SUBSCRIBE_TEMPLATE = "/app/terminal/subscribe/{terminalId}";
    
    /**
     * 批量指令执行应用端点
     */
    public static final String APP_BATCH_COMMAND_EXECUTE = "/app/batch/command/execute";
    
    // ==================== 工具方法 ====================
    
    /**
     * 构建用户通知主题路径
     * 
     * @param userId 用户ID
     * @return 用户通知主题路径
     */
    public static String buildUserNotificationTopic(String userId) {
        return USER_NOTIFICATION_TOPIC_TEMPLATE.replace("{userId}", userId);
    }
    
    /**
     * 构建组织公告主题路径
     * 
     * @param orgId 组织ID
     * @return 组织公告主题路径
     */
    public static String buildOrgAnnouncementTopic(String orgId) {
        return ORG_ANNOUNCEMENT_TOPIC_TEMPLATE.replace("{orgId}", orgId);
    }
    
    /**
     * 构建终端状态主题路径
     * 
     * @param terminalId 终端ID
     * @return 终端状态主题路径
     */
    public static String buildTerminalStatusTopic(String terminalId) {
        return TERMINAL_STATUS_TOPIC_TEMPLATE.replace("{terminalId}", terminalId);
    }
    
    /**
     * 构建批量指令进度主题路径
     * 
     * @param taskId 任务ID
     * @return 批量指令进度主题路径
     */
    public static String buildBatchCommandSummaryTopic(String taskId) {
        return BATCH_COMMAND_SUMMARY_TOPIC_TEMPLATE.replace("{taskId}", taskId);
    }
    
    /**
     * 构建用户任务进度主题路径
     * 
     * @param userId 用户ID
     * @return 用户任务进度主题路径
     */
    public static String buildUserTaskProgressTopic(String userId) {
        return USER_TASK_PROGRESS_TOPIC_TEMPLATE.replace("{userId}", userId);
    }
    
    /**
     * 构建用户指令反馈主题路径
     * 
     * @param userId 用户ID
     * @return 用户指令反馈主题路径
     */
    public static String buildUserCommandFeedbackTopic(String userId) {
        return USER_COMMAND_FEEDBACK_TOPIC_TEMPLATE.replace("{userId}", userId);
    }
    
    /**
     * 构建组织终端状态主题路径
     * 
     * @param orgId 组织ID
     * @return 组织终端状态主题路径
     */
    public static String buildOrgTerminalStatusTopic(String orgId) {
        return ORG_TERMINAL_STATUS_TOPIC_TEMPLATE.replace("{orgId}", orgId);
    }
    
    /**
     * 构建组织监控主题路径
     * 
     * @param orgId 组织ID
     * @return 组织监控主题路径
     */
    public static String buildOrgMonitoringTopic(String orgId) {
        return ORG_MONITORING_TOPIC_TEMPLATE.replace("{orgId}", orgId);
    }
    
    /**
     * 构建终端指令主题路径
     * 
     * @param terminalId 终端ID
     * @return 终端指令主题路径
     */
    public static String buildTerminalCommandTopic(String terminalId) {
        return TERMINAL_COMMAND_TOPIC_TEMPLATE.replace("{terminalId}", terminalId);
    }
    
    /**
     * 构建终端数据流主题路径
     * 
     * @param terminalId 终端ID
     * @return 终端数据流主题路径
     */
    public static String buildTerminalDataTopic(String terminalId) {
        return TERMINAL_DATA_TOPIC_TEMPLATE.replace("{terminalId}", terminalId);
    }
    
    /**
     * 构建批量指令详细进度主题路径
     * 
     * @param taskId 任务ID
     * @return 批量指令详细进度主题路径
     */
    public static String buildBatchCommandDetailedTopic(String taskId) {
        return BATCH_COMMAND_DETAILED_TOPIC_TEMPLATE.replace("{taskId}", taskId);
    }
    
    /**
     * 构建批量指令结果主题路径
     * 
     * @param taskId 任务ID
     * @return 批量指令结果主题路径
     */
    public static String buildBatchCommandResultTopic(String taskId) {
        return BATCH_COMMAND_RESULT_TOPIC_TEMPLATE.replace("{taskId}", taskId);
    }
    
    /**
     * 构建终端指令应用端点路径
     * 
     * @param terminalId 终端ID
     * @return 终端指令应用端点路径
     */
    public static String buildAppTerminalCommand(String terminalId) {
        return APP_TERMINAL_COMMAND_TEMPLATE.replace("{terminalId}", terminalId);
    }
    
    /**
     * 构建终端订阅应用端点路径
     * 
     * @param terminalId 终端ID
     * @return 终端订阅应用端点路径
     */
    public static String buildAppTerminalSubscribe(String terminalId) {
        return APP_TERMINAL_SUBSCRIBE_TEMPLATE.replace("{terminalId}", terminalId);
    }
}

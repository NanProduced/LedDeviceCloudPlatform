package org.nan.cloud.message.api.stomp;

import lombok.Getter;

/**
 * STOMP消息类型
 */
@Getter
public enum StompMessageTypes {

    /* 指令执行反馈 */
    /**
     * subType_1: SINGLE, BATCH
     * subType_2: SUCCESS, REJECT
     */
    COMMAND_FEEDBACK,

    /* 终端状态变更 */
    /**
     * ONLINE, OFFLINE, LED_STATUS, SENSOR, DOWNLOAD
     */
    TERMINAL_STATUS,

    /* 任务执行进度 */
    /**
     * subType_1: DOWNLOAD, TRANSCODE, EXPORT
     * subType_2: PROGRESS, COMPLETE, FAILED, TIMEOUT
     */
    TASK_PROGRESS,

    /* 通知 */
    /**
     * 一般是前端弹窗显示由系统给的业务相关的通知消息
     * subType_1 : user\org\...
     * subType_2 : ...
     */
    NOTIFICATION,

    /* 告警信息 */
    ALERT,

    /* 系统消息 */
    SYSTEM_MESSAGE,

    /* 组织消息 */
    ORG_MESSAGE,

    /* 监控数据 */
    MONITOR_DATA,

    /* websocket连接消息 */
    CONNECTION_STATUS,           // 连接状态消息

    SUBSCRIPTION_STATUS,        // 订阅状态消息

    HEARTBEAT,                   // 心跳消息

    TOPIC_SUBSCRIBE_FEEDBACK;   // 主题订阅反馈
}

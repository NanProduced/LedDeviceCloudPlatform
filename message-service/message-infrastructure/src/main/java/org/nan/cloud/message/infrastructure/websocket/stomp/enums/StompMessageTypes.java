package org.nan.cloud.message.infrastructure.websocket.stomp.enums;

import lombok.Getter;

/**
 * STOMP消息类型
 */
@Getter
public enum StompMessageTypes {

    /* 指令执行反馈 */
    COMMAND_FEEDBACK,

    /* 终端状态变更 */
    TERMINAL_STATUS_CHANGE,

    /* 任务执行进度 */
    TASK_PROGRESS,

    /* 通知 */
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
    HEARTBEAT;                   // 心跳消息
}

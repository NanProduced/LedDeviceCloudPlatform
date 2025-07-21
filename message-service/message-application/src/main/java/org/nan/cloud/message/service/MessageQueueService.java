package org.nan.cloud.message.service;

import org.nan.cloud.message.api.event.MessageEvent;

/**
 * 消息队列服务接口
 * 
 * 定义消息队列操作的业务接口
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageQueueService {

    /**
     * 异步发送通知消息
     *
     * 通过RabbitMQ队列异步处理消息，提高系统响应性能，
     * 支持消息持久化、重试机制和故障恢复。
     *
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    void sendNotificationAsync(String receiverId, String organizationId,
                                      String title, String content);

    /**
     * 异步发送系统消息
     *
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    void sendSystemMessageAsync(String organizationId, String title, String content);

    /**
     * 异步发送用户消息
     *
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    void sendUserMessageAsync(String senderId, String senderName, String receiverId,
                                     String organizationId, String title, String content);


    /**
     * 异步发送广播消息
     *
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param organizationId 组织ID
     * @param title 消息标题
     * @param content 消息内容
     */
    void sendBroadcastAsync(String senderId, String senderName, String organizationId,
                                   String title, String content);

    /**
     * 异步发送设备告警
     *
     * @param userId 用户ID（可选，为空时发送给整个组织）
     * @param organizationId 组织ID
     * @param deviceId 设备ID
     * @param title 告警标题
     * @param content 告警内容
     */
    void sendDeviceAlertAsync(String userId, String organizationId, String deviceId,
                                     String title, String content);
    
    /**
     * 发布自定义消息事件到队列
     * 
     * @param event 消息事件
     */
    void publishEvent(MessageEvent event);
    
    /**
     * 重新发布失败的事件
     * 
     * @param event 失败的消息事件
     */
    void republishFailedEvent(MessageEvent event);
}
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
     * 发布通知消息到队列
     * 
     * @param messageId 消息ID
     * @param title 消息标题
     * @param content 消息内容
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     */
    void publishNotification(String messageId, String title, String content, 
                           String receiverId, String organizationId);
    
    /**
     * 发布系统消息到队列
     * 
     * @param messageId 消息ID
     * @param title 消息标题
     * @param content 消息内容
     * @param organizationId 组织ID
     */
    void publishSystemMessage(String messageId, String title, String content, 
                            String organizationId);
    
    /**
     * 发布用户消息到队列
     * 
     * @param messageId 消息ID
     * @param title 消息标题
     * @param content 消息内容
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     */
    void publishUserMessage(String messageId, String title, String content,
                          String senderId, String senderName, 
                          String receiverId, String organizationId);
    
    /**
     * 发布广播消息到队列
     * 
     * @param messageId 消息ID
     * @param title 消息标题
     * @param content 消息内容
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @param organizationId 组织ID
     */
    void publishBroadcast(String messageId, String title, String content,
                        String senderId, String senderName, String organizationId);
    
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
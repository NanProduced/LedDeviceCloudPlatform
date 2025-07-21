package org.nan.cloud.message.repository;

import org.nan.cloud.message.api.event.MessageEvent;

/**
 * 消息事件仓储接口
 * 
 * 定义消息事件发布的抽象接口，遵循DDD架构的依赖倒置原则。
 * Application层定义接口，Infrastructure层提供实现。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageEventRepository {
    
    /**
     * 发布消息事件到队列
     * 
     * @param event 消息事件
     */
    void publishEvent(MessageEvent event);
    
    /**
     * 发布通知消息
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
     * 发布系统消息
     * 
     * @param messageId 消息ID
     * @param title 消息标题
     * @param content 消息内容
     * @param organizationId 组织ID
     */
    void publishSystemMessage(String messageId, String title, String content, 
                            String organizationId);
    
    /**
     * 发布用户消息
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
     * 发布广播消息
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
     * 重新发布失败的事件
     * 
     * @param event 失败的消息事件
     */
    void republishFailedEvent(MessageEvent event);
}
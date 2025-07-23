package org.nan.cloud.common.mq.producer;

import org.nan.cloud.common.mq.core.message.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 消息生产者接口
 * 
 * 提供统一的消息发送接口，支持同步/异步发送、批量发送等功能。
 * 简化RabbitMQ生产者的使用复杂度。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface MessageProducer {
    
    /**
     * 发送消息（同步）
     * 
     * @param message 消息对象
     * @return 发送结果
     */
    SendResult send(Message message);
    
    /**
     * 发送消息到指定交换机和路由键（同步）
     * 
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息对象
     * @return 发送结果
     */
    SendResult send(String exchange, String routingKey, Message message);
    
    /**
     * 发送消息（异步）
     * 
     * @param message 消息对象
     * @return 发送结果的Future
     */
    CompletableFuture<SendResult> sendAsync(Message message);
    
    /**
     * 发送消息到指定交换机和路由键（异步）
     * 
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息对象
     * @return 发送结果的Future
     */
    CompletableFuture<SendResult> sendAsync(String exchange, String routingKey, Message message);
    
    /**
     * 批量发送消息（同步）
     * 
     * @param messages 消息列表
     * @return 批量发送结果
     */
    BatchSendResult sendBatch(List<Message> messages);
    
    /**
     * 批量发送消息到指定交换机和路由键（同步）
     * 
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param messages 消息列表
     * @return 批量发送结果
     */
    BatchSendResult sendBatch(String exchange, String routingKey, List<Message> messages);
    
    /**
     * 批量发送消息（异步）
     * 
     * @param messages 消息列表
     * @return 批量发送结果的Future
     */
    CompletableFuture<BatchSendResult> sendBatchAsync(List<Message> messages);
    
    /**
     * 发送通知消息
     * 
     * @param subject 主题
     * @param payload 消息内容
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @return 发送结果
     */
    SendResult sendNotification(String subject, Object payload, String receiverId, String organizationId);
    
    /**
     * 发送系统消息
     * 
     * @param subject 主题
     * @param payload 消息内容
     * @param organizationId 组织ID
     * @return 发送结果
     */
    SendResult sendSystemMessage(String subject, Object payload, String organizationId);
    
    /**
     * 发送用户消息
     * 
     * @param subject 主题
     * @param payload 消息内容
     * @param senderId 发送者ID
     * @param receiverId 接收者ID
     * @param organizationId 组织ID
     * @return 发送结果
     */
    SendResult sendUserMessage(String subject, Object payload, String senderId, String receiverId, String organizationId);
    
    /**
     * 发送广播消息
     * 
     * @param subject 主题
     * @param payload 消息内容
     * @param senderId 发送者ID
     * @param organizationId 组织ID
     * @return 发送结果
     */
    SendResult sendBroadcast(String subject, Object payload, String senderId, String organizationId);
    
    /**
     * 发送事件消息
     * 
     * @param eventType 事件类型
     * @param eventData 事件数据
     * @param sourceSystem 源系统
     * @param targetSystem 目标系统
     * @return 发送结果
     */
    SendResult sendEvent(String eventType, Object eventData, String sourceSystem, String targetSystem);
    
    /**
     * 发送延迟消息
     * 
     * @param message 消息对象
     * @param delayMillis 延迟时间（毫秒）
     * @return 发送结果
     */
    SendResult sendDelayed(Message message, long delayMillis);
    
    /**
     * 获取生产者统计信息
     * 
     * @return 统计信息
     */
    ProducerStats getStats();
    
    /**
     * 检查生产者健康状态
     * 
     * @return 健康状态
     */
    HealthStatus getHealth();
}
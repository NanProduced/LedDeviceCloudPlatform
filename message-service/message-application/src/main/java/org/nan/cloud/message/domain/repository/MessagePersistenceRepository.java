package org.nan.cloud.message.domain.repository;

import org.nan.cloud.message.api.dto.response.MessageDetailResponse;
import org.nan.cloud.message.api.dto.response.MessageListResponse;
import org.nan.cloud.message.api.event.MessageEvent;

import java.util.List;
import java.util.Optional;

/**
 * 消息持久化仓储接口
 * 
 * 定义消息持久化的领域接口，application层依赖此抽象接口，
 * infrastructure层提供具体实现，遵循DDD架构的依赖倒置原则。
 * 
 * 核心功能：
 * - 消息的完整生命周期管理
 * - 多数据源的统一访问接口
 * - 读写性能优化的抽象定义
 * - 数据一致性保障的接口契约
 * 
 * @author Nan
 * @since 1.0.0
 */
public interface MessagePersistenceRepository {
    
    /**
     * 保存完整消息
     * 
     * @param event 消息事件对象
     */
    void saveMessage(MessageEvent event);
    
    /**
     * 批量保存消息
     * 
     * @param events 消息事件列表
     */
    void batchSaveMessages(List<MessageEvent> events);
    
    /**
     * 获取完整消息信息
     * 
     * @param messageId 消息ID
     * @return 完整消息信息
     */
    Optional<MessageDetailResponse> getCompleteMessage(String messageId);
    
    /**
     * 分页查询用户消息
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param messageType 消息类型（可选）
     * @param status 消息状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页消息列表
     */
    MessageListResponse getMessagesByPage(String receiverId, String organizationId, 
                                        String messageType, String status, 
                                        int page, int size);
    
    /**
     * 查询用户未读消息
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 未读消息列表
     */
    List<MessageDetailResponse> getUnreadMessages(String receiverId, String organizationId, int limit);
    
    /**
     * 更新消息状态
     * 
     * @param messageId 消息ID
     * @param newStatus 新状态
     */
    void updateMessageStatus(String messageId, String newStatus);
    
    /**
     * 批量标记消息为已读
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param messageIds 消息ID列表
     */
    void markMessagesAsRead(String receiverId, String organizationId, List<String> messageIds);
    
    /**
     * 获取用户未读消息数量
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @return 未读消息数量
     */
    Long getUnreadMessageCount(String receiverId, String organizationId);
}
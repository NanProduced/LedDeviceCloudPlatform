package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryBroadcastMessageRequest;
import org.nan.cloud.core.api.DTO.res.BroadcastMessageResponse;

import java.util.List;
import java.util.Map;

/**
 * 广播消息服务接口
 * 
 * 提供广播消息查询、统计和管理功能
 * 基于分离存储架构：消息内容 + 用户已读状态
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface BroadcastMessageService {
    
    /**
     * 分页查询用户广播消息
     * 
     * @param pageRequest 分页查询请求
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 分页消息结果（包含已读状态）
     */
    PageVO<BroadcastMessageResponse> getUserBroadcastMessages(
        PageRequestDTO<QueryBroadcastMessageRequest> pageRequest, 
        Long userId, Long orgId);
    
    /**
     * 获取用户未读广播消息总数
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 未读消息数量
     */
    Long getUnreadCount(Long userId, Long orgId);
    
    /**
     * 获取用户按消息类型分组的未读广播消息数量
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 按消息类型分组的未读数量映射
     */
    Map<String, Long> getUnreadCountByType(Long userId, Long orgId);
    
    /**
     * 获取用户登录时需要推送的未读通知消息
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param limit 限制数量
     * @return 未读通知消息列表
     */
    List<BroadcastMessageResponse> getUnreadNotificationsForLogin(Long userId, Long orgId, int limit);
    
    /**
     * 根据消息ID获取消息详情
     * 
     * @param messageId 消息ID
     * @param userId 用户ID（权限验证和已读状态）
     * @return 消息详情（包含已读状态）
     */
    BroadcastMessageResponse getMessageById(String messageId, Long userId);
    
    /**
     * 标记消息已读
     * 
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    void markMessageAsRead(String messageId, Long userId, Long orgId);
    
    /**
     * 批量标记消息已读
     * 
     * @param messageIds 消息ID列表
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    void batchMarkMessagesAsRead(List<String> messageIds, Long userId, Long orgId);
    
    /**
     * 清除用户未读消息缓存
     * 
     * @param userId 用户ID
     */
    void clearUnreadCountCache(Long userId);
    
    /**
     * 批量清除用户未读消息缓存
     * 
     * @param userIds 用户ID列表
     */
    void batchClearUnreadCountCache(List<Long> userIds);
}
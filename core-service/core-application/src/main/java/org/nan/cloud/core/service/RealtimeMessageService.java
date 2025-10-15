package org.nan.cloud.core.service;

import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryRealtimeMessageRequest;
import org.nan.cloud.core.api.DTO.res.RealtimeMessageResponse;

import java.util.Map;

/**
 * 实时消息服务接口
 * 
 * 提供实时消息查询、统计和管理功能
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface RealtimeMessageService {
    
    /**
     * 分页查询用户实时消息
     * 
     * @param pageRequest 分页查询请求
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 分页消息结果
     */
    PageVO<RealtimeMessageResponse> getUserMessages(PageRequestDTO<QueryRealtimeMessageRequest> pageRequest, 
                                                   Long userId, Long orgId);
    
    /**
     * 获取用户未读消息总数
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 未读消息数量
     */
    Long getUnreadCount(Long userId, Long orgId);
    
    /**
     * 获取用户按消息类型分组的未读消息数量
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 按消息类型分组的未读数量映射
     */
    Map<String, Long> getUnreadCountByType(Long userId, Long orgId);
    
    /**
     * 根据消息ID获取消息详情
     * 
     * @param messageId 消息ID
     * @param userId 用户ID（权限验证）
     * @return 消息文档
     */
    RealtimeMessageDocument getMessageById(String messageId, Long userId);
    
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
    void batchClearUnreadCountCache(java.util.List<Long> userIds);
}

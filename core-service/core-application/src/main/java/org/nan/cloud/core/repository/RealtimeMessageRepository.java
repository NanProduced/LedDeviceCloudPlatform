package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.nan.cloud.common.basic.model.PageVO;

import java.util.Map;
import java.util.Optional;

/**
 * 实时消息Repository接口
 * 
 * 定义实时消息数据访问层接口，由Infrastructure层实现
 * Application模块不引入Spring Data依赖，只使用项目通用的PageVO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface RealtimeMessageRepository {

    /**
     * 查询用户的实时消息（可以添加类型筛选）
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param messageType 消息类型
     * @param onlyUnread 只查未读
     * @param orgId 组织ID
     * @param userId 用户ID
     * @return 分页消息结果
     */
    PageVO<RealtimeMessageDocument> findUserMessages(int pageNum, int pageSize, String messageType, boolean onlyUnread, Long orgId, Long userId);

    /**
     * 计算用户未读消息
     * @param orgId 组织ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    long countUnreadMessages(Long orgId, Long userId);

    /**
     * 分类型计算未读消息数量
     * @param orgId 组织ID
     * @param userId 用户ID
     * @return 按消息类型分组的未读数量映射
     */
    Map<String, Long> countUnreadMessagesByType(Long orgId, Long userId);
    
    /**
     * 根据消息ID和用户ID查找消息（用于权限验证）
     * 
     * @param messageId 消息ID
     * @param userId 用户ID
     * @return 消息文档（如果存在且属于该用户）
     */
    Optional<RealtimeMessageDocument> findByMessageIdAndUserId(String messageId, Long userId);
    
    /**
     * 根据MongoDB文档ID查找消息
     * 
     * @param id MongoDB文档ID
     * @return 消息文档
     */
    Optional<RealtimeMessageDocument> findById(String id);
}

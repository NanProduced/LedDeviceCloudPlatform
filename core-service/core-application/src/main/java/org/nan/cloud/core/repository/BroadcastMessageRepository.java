package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.nan.cloud.common.basic.model.PageVO;

import java.util.List;
import java.util.Optional;

/**
 * 广播消息Repository接口
 * 
 * 定义广播消息数据访问层接口，由Infrastructure层实现
 * Application模块不引入Spring Data依赖，只使用项目通用的PageVO
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface BroadcastMessageRepository {

    /**
     * 查询用户可见的广播消息（分页）
     * 
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param messageType 消息类型筛选
     * @param scope 消息范围筛选（SYSTEM/ORG）
     * @param orgId 组织ID
     * @return 分页消息结果
     */
    PageVO<BroadcastMessageDocument> findUserVisibleMessages(
        int pageNum, int pageSize, String messageType, String scope, Long orgId);

    /**
     * 查询用户未读的通知消息（登录推送用）
     * 
     * @param userId 用户ID
     * @param orgId 组织ID  
     * @param limit 限制数量
     * @return 未读通知消息列表（按优先级和时间排序）
     */
    List<BroadcastMessageDocument> findUnreadNotificationsForUser(Long userId, Long orgId, int limit);

    /**
     * 根据消息ID查找消息
     * 
     * @param messageId 消息ID
     * @return 消息文档
     */
    Optional<BroadcastMessageDocument> findByMessageId(String messageId);

    /**
     * 根据MongoDB文档ID查找消息
     * 
     * @param id MongoDB文档ID
     * @return 消息文档
     */
    Optional<BroadcastMessageDocument> findById(String id);

    /**
     * 查询组织内的广播消息（用于统计）
     * 
     * @param orgId 组织ID
     * @param messageType 消息类型筛选
     * @return 广播消息列表
     */
    List<BroadcastMessageDocument> findByOrgId(Long orgId, String messageType);

    /**
     * 查询系统级广播消息（用于统计）
     * 
     * @param messageType 消息类型筛选
     * @return 系统广播消息列表
     */
    List<BroadcastMessageDocument> findSystemMessages(String messageType);
}
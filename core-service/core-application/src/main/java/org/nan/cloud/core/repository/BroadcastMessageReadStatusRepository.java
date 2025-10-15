package org.nan.cloud.core.repository;

import org.nan.cloud.common.basic.domain.BroadcastMessageReadStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 广播消息已读状态Repository接口
 * 
 * 定义广播消息已读状态数据访问层接口，由Infrastructure层实现
 * Application模块不引入Spring Data依赖
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface BroadcastMessageReadStatusRepository {

    /**
     * 批量查询用户已读的消息ID
     * 
     * @param userId 用户ID
     * @param messageIds 消息ID列表
     * @return 用户已读的消息ID集合
     */
    Set<String> findReadMessageIds(Long userId, List<String> messageIds);

    /**
     * 检查用户是否已读某条消息
     * 
     * @param userId 用户ID
     * @param messageId 消息ID
     * @return 是否已读
     */
    boolean isMessageRead(Long userId, String messageId);

    /**
     * 统计用户未读广播消息数量
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 未读消息数量
     */
    long countUnreadMessages(Long userId, Long orgId);

    /**
     * 按类型统计用户未读广播消息数量
     * 
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 按消息类型分组的未读数量映射
     */
    Map<String, Long> countUnreadMessagesByType(Long userId, Long orgId);

    /**
     * 保存已读状态（支持upsert）
     * 
     * @param readStatus 已读状态记录
     * @return 保存的记录ID
     */
    String upsert(BroadcastMessageReadStatus readStatus);

    /**
     * 批量保存已读状态
     * 
     * @param readStatusList 已读状态记录列表
     * @return 保存成功的记录数量
     */
    int batchUpsert(List<BroadcastMessageReadStatus> readStatusList);

    /**
     * 根据复合ID查找已读状态
     * 
     * @param id 复合ID (messageId_userId)
     * @return 已读状态记录
     */
    Optional<BroadcastMessageReadStatus> findById(String id);

    /**
     * 删除已读状态记录
     * 
     * @param id 复合ID
     * @return 是否删除成功
     */
    boolean deleteById(String id);

    /**
     * 查询用户的所有已读记录
     * 
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 已读状态记录列表
     */
    List<BroadcastMessageReadStatus> findByUserId(Long userId, int limit);
}
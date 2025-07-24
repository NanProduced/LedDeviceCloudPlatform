package org.nan.cloud.terminal.infrastructure.persistence.mongodb.repository;

import org.nan.cloud.terminal.infrastructure.persistence.mongodb.document.OfflineMessageDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 离线消息Repository
 * 
 * 提供离线消息的持久化操作，支持按设备ID查询、优先级排序等功能。
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Repository
public interface OfflineMessageRepository extends MongoRepository<OfflineMessageDocument, String> {
    
    /**
     * 查询指定终端的待推送消息（按优先级和创建时间排序）
     * 
     * @param did 终端ID
     * @param status 消息状态
     * @param pageable 分页参数
     * @return 离线消息列表
     */
    @Query("{'did': ?0, 'status': ?1}")
    Page<OfflineMessageDocument> findPendingMessagesByDeviceId(
            String did, 
            OfflineMessageDocument.MessageStatus status, 
            Pageable pageable);
    
    /**
     * 查询指定终端的所有待推送消息（按优先级倒序，创建时间正序）
     * 
     * @param did 终端ID
     * @return 离线消息列表
     */
    List<OfflineMessageDocument> findByDidAndStatusOrderByPriorityDescCreateTimeAsc(
            String did, 
            OfflineMessageDocument.MessageStatus status);
    
    /**
     * 查询指定组织的所有待推送消息
     * 
     * @param oid 组织ID
     * @param status 消息状态
     * @return 离线消息列表
     */
    List<OfflineMessageDocument> findByOidAndStatus(
            String oid, 
            OfflineMessageDocument.MessageStatus status);
    
    /**
     * 统计指定终端的待推送消息数量
     * 
     * @param did 终端ID
     * @param status 消息状态
     * @return 消息数量
     */
    long countByDidAndStatus(String did, OfflineMessageDocument.MessageStatus status);
    
    /**
     * 查询过期消息（用于清理）
     * 
     * @param currentTime 当前时间
     * @return 过期消息列表
     */
    @Query("{'expireTime': {'$lt': ?0}}")
    List<OfflineMessageDocument> findExpiredMessages(LocalDateTime currentTime);
    
    /**
     * 删除指定终端的已投递消息（清理历史数据）
     * 
     * @param did 终端ID
     * @param status 消息状态
     * @param beforeTime 时间截止点
     * @return 删除数量
     */
    long deleteByDidAndStatusAndDeliveredTimeBefore(
            String did, 
            OfflineMessageDocument.MessageStatus status, 
            LocalDateTime beforeTime);
    
    /**
     * 查询推送失败且可重试的消息
     * 
     * @param maxRetryCount 最大重试次数
     * @param currentTime 当前时间
     * @return 可重试消息列表
     */
    @Query("{'status': 'FAILED', 'retryCount': {'$lt': ?0}, 'expireTime': {'$gt': ?1}}")
    List<OfflineMessageDocument> findRetryableFailedMessages(int maxRetryCount, LocalDateTime currentTime);
    
    /**
     * 批量更新消息状态
     * 
     * @param messageIds 消息ID列表
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    @Query("{'_id': {'$in': ?0}, 'status': ?1}")
    void updateStatusBatch(List<String> messageIds, 
                          OfflineMessageDocument.MessageStatus oldStatus, 
                          OfflineMessageDocument.MessageStatus newStatus);
}
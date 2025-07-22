package org.nan.cloud.message.infrastructure.mongodb.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.nan.cloud.message.infrastructure.mongodb.document.MessageDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息详细内容Repository接口
 * 
 * 提供消息详细内容的MongoDB数据访问功能，支持复杂查询和聚合操作。
 * 优化查询性能，支持大数据量消息内容的高效存储和检索。
 * 
 * 核心功能：
 * - 消息内容的CRUD操作
 * - 按组织和用户查询消息
 * - 消息内容全文搜索
 * - 批量操作和聚合查询
 * - TTL过期数据清理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Repository
public interface MessageDetailMongoRepository extends MongoRepository<MessageDetail, String> {
    
    /**
     * 根据消息ID查询消息详情
     * 
     * @param messageId 消息ID
     * @return 消息详情（可选）
     */
    Optional<MessageDetail> findByMessageId(String messageId);
    
    /**
     * 批量根据消息ID查询消息详情
     * 
     * 用于与MySQL查询结果关联，获取完整的消息信息。
     * 按照messageId顺序返回，便于前端显示。
     * 
     * @param messageIds 消息ID列表
     * @return 消息详情列表
     */
    List<MessageDetail> findByMessageIdInOrderByCreatedTimeDesc(List<String> messageIds);
    
    /**
     * 查询用户在组织内的消息详情
     * 
     * 支持多租户数据隔离，确保用户只能查看自己组织内的消息。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 分页消息详情
     */
    Page<MessageDetail> findByReceiverIdAndOrganizationIdOrderByCreatedTimeDesc(
            String receiverId, String organizationId, Pageable pageable);
    
    /**
     * 根据消息类型查询组织内的消息
     * 
     * 用于消息分析和统计，按消息类型进行数据聚合。
     * 
     * @param organizationId 组织ID
     * @param messageType 消息类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页消息详情
     */
    Page<MessageDetail> findByOrganizationIdAndMessageTypeAndCreatedTimeBetween(
            String organizationId, String messageType, 
            LocalDateTime startTime, LocalDateTime endTime, 
            Pageable pageable);
    
    /**
     * 全文搜索消息内容
     * 
     * 在标题和内容中搜索关键词，支持模糊匹配。
     * 使用MongoDB的文本索引进行高效搜索。
     * 
     * @param organizationId 组织ID（数据隔离）
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的消息列表
     */
    @Query("{ 'organizationId': ?0, $text: { $search: ?1 } }")
    Page<MessageDetail> searchByKeyword(String organizationId, String keyword, Pageable pageable);
    
    /**
     * 查询包含附件的消息
     * 
     * 用于附件管理和存储空间统计。
     * 
     * @param organizationId 组织ID
     * @param pageable 分页参数
     * @return 包含附件的消息列表
     */
    @Query("{ 'organizationId': ?0, 'attachments': { $exists: true, $not: { $size: 0 } } }")
    Page<MessageDetail> findMessagesWithAttachments(String organizationId, Pageable pageable);
    
    /**
     * 查询指定优先级的消息
     * 
     * 用于优先级消息的单独处理和统计。
     * 
     * @param organizationId 组织ID
     * @param priority 消息优先级
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 指定优先级的消息列表
     */
    List<MessageDetail> findByOrganizationIdAndPriorityAndCreatedTimeBetween(
            String organizationId, Integer priority, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计组织消息数量
     * 
     * 按消息类型统计组织内的消息数量，用于数据分析。
     * 
     * @param organizationId 组织ID
     * @param startTime 统计开始时间
     * @param endTime 统计结束时间
     * @return 消息数量
     */
    long countByOrganizationIdAndCreatedTimeBetween(
            String organizationId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查询即将过期的消息
     * 
     * 用于过期消息的预警和清理准备。
     * 
     * @param beforeTime 过期时间点
     * @param limit 限制数量
     * @return 即将过期的消息列表
     */
    @Query("{ 'ttl': { $exists: true, $lt: ?0 } }")
    List<MessageDetail> findExpiringMessages(LocalDateTime beforeTime, Pageable pageable);
    
    /**
     * 删除已过期的消息
     * 
     * 物理删除已过期的消息，释放存储空间。
     * MongoDB的TTL索引会自动处理，此方法用于手动清理。
     * 
     * @param beforeTime 过期时间点
     * @return 删除的消息数量
     */
    long deleteByTtlBefore(LocalDateTime beforeTime);
    
    /**
     * 查询用户最近的消息
     * 
     * 用于用户上线时推送最近的重要消息详情。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 最近消息列表
     */
    @Query("{ 'receiverId': ?0, 'organizationId': ?1, 'priority': { $gte: 2 } }")
    List<MessageDetail> findRecentImportantMessages(String receiverId, String organizationId, Pageable pageable);
    
    /**
     * 统计用户消息附件总大小
     * 
     * 用于存储配额管理和用户使用情况统计。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @return 附件总大小（字节）
     */
    @Query(value = "{ 'receiverId': ?0, 'organizationId': ?1, 'attachments': { $exists: true } }", 
           fields = "{ 'attachments.fileSize': 1 }")
    List<MessageDetail> findUserAttachmentSizes(String receiverId, String organizationId);
    
    /**
     * 查询包含特定标签的消息
     * 
     * 支持消息标签分类和快速筛选。
     * 
     * @param organizationId 组织ID
     * @param tags 标签列表
     * @param pageable 分页参数
     * @return 包含指定标签的消息
     */
    @Query("{ 'organizationId': ?0, 'tags': { $in: ?1 } }")
    Page<MessageDetail> findByOrganizationIdAndTagsIn(String organizationId, List<String> tags, Pageable pageable);
    
    /**
     * 聚合查询消息统计信息
     * 
     * 使用MongoDB的聚合管道进行复杂统计分析。
     * 
     * @param organizationId 组织ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 聚合统计结果
     */
    @Query("{ $match: { 'organizationId': ?0, 'createdTime': { $gte: ?1, $lte: ?2 } } }")
    List<MessageDetail> findForAggregation(String organizationId, LocalDateTime startTime, LocalDateTime endTime);
}
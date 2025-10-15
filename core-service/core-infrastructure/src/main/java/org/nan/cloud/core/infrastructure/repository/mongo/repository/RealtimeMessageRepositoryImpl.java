package org.nan.cloud.core.infrastructure.repository.mongo.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.repository.RealtimeMessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 实时消息Repository MongoDB实现类
 * 
 * 负责实时消息在MongoDB中的查询操作
 * 参考MaterialMetadataRepositoryImpl的实现模式
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RealtimeMessageRepositoryImpl implements RealtimeMessageRepository {
    
    private final MongoTemplate mongoTemplate;
    
    private static final String COLLECTION_NAME = "realtime_message";
    
    @Override
    public PageVO<RealtimeMessageDocument> findUserMessages(int pageNum, int pageSize, String messageType, 
                                                           boolean onlyUnread, Long orgId, Long userId) {
        try {
            log.debug("查询用户实时消息 - 用户ID: {}, 组织ID: {}, 页码: {}, 大小: {}, 类型: {}, 只查未读: {}", 
                    userId, orgId, pageNum, pageSize, messageType, onlyUnread);
            
            // 构建查询条件
            Criteria criteria = buildUserMessagesCriteria(userId, orgId, messageType, onlyUnread);
            
            // 构建查询对象，按创建时间倒序
            Query query = new Query(criteria);
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createTime"));
            
            // 计算总数
            long total = mongoTemplate.count(query, RealtimeMessageDocument.class, COLLECTION_NAME);
            
            // 分页查询
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            List<RealtimeMessageDocument> messages = mongoTemplate.find(query, RealtimeMessageDocument.class, COLLECTION_NAME);
            
            // 构建PageVO结果
            PageVO<RealtimeMessageDocument> result = PageVO.<RealtimeMessageDocument>builder()
                    .records(messages)
                    .total(total)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) total / pageSize))
                    .build();
            
            log.debug("查询用户实时消息完成 - 用户ID: {}, 返回数量: {}/{}", userId, messages.size(), total);
            return result;
            
        } catch (Exception e) {
            log.error("查询用户实时消息失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return PageVO.<RealtimeMessageDocument>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages(0)
                    .build();
        }
    }
    
    
    @Override
    public long countUnreadMessages(Long orgId, Long userId) {
        try {
            log.debug("统计用户未读消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            Criteria criteria = Criteria.where("uid").is(userId)
                    .and("oid").is(orgId)
                    .and("isRead").is(false);
            
            Query query = new Query(criteria);
            long count = mongoTemplate.count(query, RealtimeMessageDocument.class, COLLECTION_NAME);
            
            log.debug("统计用户未读消息数量完成 - 用户ID: {}, 数量: {}", userId, count);
            return count;
            
        } catch (Exception e) {
            log.error("统计用户未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Long> countUnreadMessagesByType(Long orgId, Long userId) {
        try {
            log.debug("按类型统计用户未读消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            // 使用MongoDB聚合查询按消息类型分组统计
            Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                    Criteria.where("uid").is(userId)
                            .and("oid").is(orgId)
                            .and("isRead").is(false)
                ),
                Aggregation.group("messageType").count().as("count"),
                Aggregation.project("count").and("messageType").previousOperation()
            );
            
            AggregationResults<UnreadCountByTypeResult> results = mongoTemplate.aggregate(
                aggregation, COLLECTION_NAME, UnreadCountByTypeResult.class);
            
            Map<String, Long> countMap = results.getMappedResults().stream()
                    .collect(Collectors.toMap(
                        UnreadCountByTypeResult::getMessageType,
                        UnreadCountByTypeResult::getCount
                    ));
            
            log.debug("按类型统计用户未读消息数量完成 - 用户ID: {}, 结果: {}", userId, countMap);
            return countMap;
            
        } catch (Exception e) {
            log.error("按类型统计用户未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public Optional<RealtimeMessageDocument> findByMessageIdAndUserId(String messageId, Long userId) {
        try {
            log.debug("根据消息ID和用户ID查询消息 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            Query query = new Query(
                Criteria.where("messageId").is(messageId)
                        .and("uid").is(userId)
            );
            
            RealtimeMessageDocument message = mongoTemplate.findOne(query, RealtimeMessageDocument.class, COLLECTION_NAME);
            
            if (message != null) {
                log.debug("根据消息ID和用户ID查询消息成功 - 消息ID: {}, 用户ID: {}", messageId, userId);
            } else {
                log.debug("根据消息ID和用户ID查询消息为空 - 消息ID: {}, 用户ID: {}", messageId, userId);
            }
            
            return Optional.ofNullable(message);
            
        } catch (Exception e) {
            log.error("根据消息ID和用户ID查询消息失败 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<RealtimeMessageDocument> findById(String id) {
        try {
            log.debug("根据ID查询消息 - ID: {}", id);
            
            RealtimeMessageDocument message = mongoTemplate.findById(id, RealtimeMessageDocument.class, COLLECTION_NAME);
            
            if (message != null) {
                log.debug("根据ID查询消息成功 - ID: {}", id);
            } else {
                log.debug("根据ID查询消息为空 - ID: {}", id);
            }
            
            return Optional.ofNullable(message);
            
        } catch (Exception e) {
            log.error("根据ID查询消息失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 构建用户消息查询条件
     */
    private Criteria buildUserMessagesCriteria(Long userId, Long orgId, String messageType, boolean onlyUnread) {
        Criteria criteria = Criteria.where("uid").is(userId);
        
        if (orgId != null) {
            criteria.and("oid").is(orgId);
        }
        
        if (messageType != null && !messageType.trim().isEmpty()) {
            criteria.and("messageType").is(messageType);
        }
        
        if (onlyUnread) {
            criteria.and("isRead").is(false);
        }
        
        return criteria;
    }
    
    /**
     * 聚合查询结果映射类
     */
    public static class UnreadCountByTypeResult {
        private String messageType;
        private Long count;
        
        public String getMessageType() {
            return messageType;
        }
        
        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
        
        public Long getCount() {
            return count;
        }
        
        public void setCount(Long count) {
            this.count = count;
        }
    }
}

package org.nan.cloud.core.infrastructure.repository.mongo.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageReadStatus;
import org.nan.cloud.core.repository.BroadcastMessageReadStatusRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 广播消息已读状态Repository MongoDB实现类
 * 
 * 负责广播消息已读状态在MongoDB中的查询和操作
 * 支持分离存储架构下的已读状态管理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BroadcastMessageReadStatusRepositoryImpl implements BroadcastMessageReadStatusRepository {
    
    private final MongoTemplate mongoTemplate;
    
    private static final String COLLECTION_NAME = "broadcast_message_read_status";
    
    @Override
    public Set<String> findReadMessageIds(Long userId, List<String> messageIds) {
        try {
            log.debug("批量查询用户已读消息ID - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
            
            Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("messageId").in(messageIds)
            );
            query.fields().include("messageId");
            
            List<BroadcastMessageReadStatus> readStatuses = mongoTemplate.find(
                query, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            Set<String> readMessageIds = readStatuses.stream()
                    .map(BroadcastMessageReadStatus::getMessageId)
                    .collect(Collectors.toSet());
            
            log.debug("批量查询用户已读消息ID完成 - 用户ID: {}, 已读数量: {}/{}", 
                    userId, readMessageIds.size(), messageIds.size());
            
            return readMessageIds;
            
        } catch (Exception e) {
            log.error("批量查询用户已读消息ID失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return new HashSet<>();
        }
    }
    
    @Override
    public boolean isMessageRead(Long userId, String messageId) {
        try {
            log.debug("检查消息是否已读 - 用户ID: {}, 消息ID: {}", userId, messageId);
            
            Query query = new Query(
                Criteria.where("userId").is(userId)
                        .and("messageId").is(messageId)
            );
            
            boolean exists = mongoTemplate.exists(query, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            log.debug("检查消息是否已读完成 - 用户ID: {}, 消息ID: {}, 结果: {}", userId, messageId, exists);
            return exists;
            
        } catch (Exception e) {
            log.error("检查消息是否已读失败 - 用户ID: {}, 消息ID: {}, 错误: {}", userId, messageId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public long countUnreadMessages(Long userId, Long orgId) {
        try {
            log.debug("统计用户未读广播消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            // 这里需要与BroadcastMessageRepository配合，统计用户可见但未读的消息数量
            // 暂时返回0，具体实现需要根据业务逻辑调整
            // TODO: 实现复杂的未读消息统计逻辑
            
            Query readQuery = new Query(Criteria.where("userId").is(userId));
            long readCount = mongoTemplate.count(readQuery, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            log.debug("统计用户未读广播消息数量完成 - 用户ID: {}, 已读数量: {}", userId, readCount);
            
            // 简化实现：返回0（实际应用中需要与消息总数比较）
            return 0;
            
        } catch (Exception e) {
            log.error("统计用户未读广播消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public Map<String, Long> countUnreadMessagesByType(Long userId, Long orgId) {
        try {
            log.debug("按类型统计用户未读广播消息数量 - 用户ID: {}, 组织ID: {}", userId, orgId);
            
            // 暂时返回空Map，具体实现需要复杂的聚合查询
            // TODO: 实现按类型统计未读消息的复杂逻辑
            
            log.debug("按类型统计用户未读广播消息数量完成 - 用户ID: {}", userId);
            return Collections.emptyMap();
            
        } catch (Exception e) {
            log.error("按类型统计用户未读广播消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public String upsert(BroadcastMessageReadStatus readStatus) {
        try {
            log.debug("保存广播消息已读状态 - 消息ID: {}, 用户ID: {}", 
                    readStatus.getMessageId(), readStatus.getUserId());
            
            Query query = new Query(Criteria.where("id").is(readStatus.getId()));
            
            Update update = new Update()
                    .set("messageId", readStatus.getMessageId())
                    .set("userId", readStatus.getUserId())
                    .set("orgId", readStatus.getOrgId())
                    .set("readAt", readStatus.getReadAt());
            
            // 执行upsert操作
            mongoTemplate.upsert(query, update, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            log.info("保存广播消息已读状态成功 - 消息ID: {}, 用户ID: {}", 
                    readStatus.getMessageId(), readStatus.getUserId());
            
            return readStatus.getId();
            
        } catch (Exception e) {
            log.error("保存广播消息已读状态失败 - 消息ID: {}, 用户ID: {}, 错误: {}", 
                    readStatus.getMessageId(), readStatus.getUserId(), e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public int batchUpsert(List<BroadcastMessageReadStatus> readStatusList) {
        if (readStatusList == null || readStatusList.isEmpty()) {
            return 0;
        }
        
        try {
            log.debug("批量保存广播消息已读状态 - 数量: {}", readStatusList.size());
            
            int successCount = 0;
            for (BroadcastMessageReadStatus readStatus : readStatusList) {
                String result = upsert(readStatus);
                if (result != null) {
                    successCount++;
                }
            }
            
            log.info("批量保存广播消息已读状态完成 - 成功: {}/{}", successCount, readStatusList.size());
            return successCount;
            
        } catch (Exception e) {
            log.error("批量保存广播消息已读状态失败 - 数量: {}, 错误: {}", 
                    readStatusList.size(), e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public Optional<BroadcastMessageReadStatus> findById(String id) {
        try {
            log.debug("根据ID查询广播消息已读状态 - ID: {}", id);
            
            BroadcastMessageReadStatus readStatus = mongoTemplate.findById(id, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            if (readStatus != null) {
                log.debug("根据ID查询广播消息已读状态成功 - ID: {}", id);
            } else {
                log.debug("根据ID查询广播消息已读状态为空 - ID: {}", id);
            }
            
            return Optional.ofNullable(readStatus);
            
        } catch (Exception e) {
            log.error("根据ID查询广播消息已读状态失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public boolean deleteById(String id) {
        try {
            log.debug("删除广播消息已读状态 - ID: {}", id);
            
            Query query = new Query(Criteria.where("id").is(id));
            mongoTemplate.remove(query, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            log.info("删除广播消息已读状态成功 - ID: {}", id);
            return true;
            
        } catch (Exception e) {
            log.error("删除广播消息已读状态失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<BroadcastMessageReadStatus> findByUserId(Long userId, int limit) {
        try {
            log.debug("查询用户的已读记录 - 用户ID: {}, 限制: {}", userId, limit);
            
            Query query = new Query(Criteria.where("userId").is(userId));
            query.with(Sort.by(Sort.Direction.DESC, "readAt"));
            query.limit(limit);
            
            List<BroadcastMessageReadStatus> readStatuses = mongoTemplate.find(
                query, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            log.debug("查询用户的已读记录完成 - 用户ID: {}, 返回数量: {}", userId, readStatuses.size());
            return readStatuses;
            
        } catch (Exception e) {
            log.error("查询用户的已读记录失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
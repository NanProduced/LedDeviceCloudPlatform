package org.nan.cloud.core.infrastructure.repository.mongo.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.repository.BroadcastMessageRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 广播消息Repository MongoDB实现类
 * 
 * 负责广播消息内容在MongoDB中的查询操作
 * 参考MaterialMetadataRepositoryImpl的实现模式
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BroadcastMessageRepositoryImpl implements BroadcastMessageRepository {
    
    private final MongoTemplate mongoTemplate;
    
    private static final String COLLECTION_NAME = "broadcast_message";
    
    @Override
    public PageVO<BroadcastMessageDocument> findUserVisibleMessages(
        int pageNum, int pageSize, String messageType, String scope, Long orgId) {
        
        try {
            log.debug("查询用户可见广播消息 - 组织ID: {}, 页码: {}, 大小: {}, 类型: {}, 范围: {}", 
                    orgId, pageNum, pageSize, messageType, scope);
            
            // 构建查询条件
            Criteria criteria = buildUserVisibleCriteria(orgId, messageType, scope);
            
            // 构建查询对象，按时间倒序
            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
            
            // 计算总数
            long total = mongoTemplate.count(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            // 分页查询
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            List<BroadcastMessageDocument> messages = mongoTemplate.find(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            // 构建PageVO结果
            PageVO<BroadcastMessageDocument> result = PageVO.<BroadcastMessageDocument>builder()
                    .records(messages)
                    .total(total)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages((int) Math.ceil((double) total / pageSize))
                    .build();
            
            log.debug("查询用户可见广播消息完成 - 组织ID: {}, 返回数量: {}/{}", orgId, messages.size(), total);
            return result;
            
        } catch (Exception e) {
            log.error("查询用户可见广播消息失败 - 组织ID: {}, 错误: {}", orgId, e.getMessage(), e);
            return PageVO.<BroadcastMessageDocument>builder()
                    .records(Collections.emptyList())
                    .total(0L)
                    .pageNum(pageNum)
                    .pageSize(pageSize)
                    .totalPages(0)
                    .build();
        }
    }
    
    @Override
    public List<BroadcastMessageDocument> findUnreadNotificationsForUser(Long userId, Long orgId, int limit) {
        try {
            log.debug("查询用户未读通知消息 - 用户ID: {}, 组织ID: {}, 限制: {}", userId, orgId, limit);
            
            // 构建查询条件：用户可见且未过期的消息
            Criteria criteria = buildUserVisibleCriteria(orgId, null, null);
            
            // 添加未过期条件
            criteria.orOperator(
                Criteria.where("expiredAt").is(null),
                Criteria.where("expiredAt").gte(LocalDateTime.now())
            );
            
            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
            query.limit(limit);
            
            List<BroadcastMessageDocument> messages = mongoTemplate.find(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            log.debug("查询用户未读通知消息完成 - 用户ID: {}, 返回数量: {}", userId, messages.size());
            return messages;
            
        } catch (Exception e) {
            log.error("查询用户未读通知消息失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public Optional<BroadcastMessageDocument> findByMessageId(String messageId) {
        try {
            log.debug("根据消息ID查询广播消息 - 消息ID: {}", messageId);
            
            Query query = new Query(Criteria.where("messageId").is(messageId));
            BroadcastMessageDocument message = mongoTemplate.findOne(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            if (message != null) {
                log.debug("根据消息ID查询广播消息成功 - 消息ID: {}", messageId);
            } else {
                log.debug("根据消息ID查询广播消息为空 - 消息ID: {}", messageId);
            }
            
            return Optional.ofNullable(message);
            
        } catch (Exception e) {
            log.error("根据消息ID查询广播消息失败 - 消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<BroadcastMessageDocument> findById(String id) {
        try {
            log.debug("根据ID查询广播消息 - ID: {}", id);
            
            BroadcastMessageDocument message = mongoTemplate.findById(id, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            if (message != null) {
                log.debug("根据ID查询广播消息成功 - ID: {}", id);
            } else {
                log.debug("根据ID查询广播消息为空 - ID: {}", id);
            }
            
            return Optional.ofNullable(message);
            
        } catch (Exception e) {
            log.error("根据ID查询广播消息失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<BroadcastMessageDocument> findByOrgId(Long orgId, String messageType) {
        try {
            log.debug("查询组织广播消息 - 组织ID: {}, 类型: {}", orgId, messageType);
            
            Criteria criteria = Criteria.where("scope").is("ORG");
            
            // 检查目标组织列表
            criteria.orOperator(
                Criteria.where("oid").is(orgId),
                Criteria.where("targetOid").in(orgId)
            );
            
            if (messageType != null && !messageType.trim().isEmpty()) {
                criteria.and("messageType").is(messageType);
            }
            
            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
            
            List<BroadcastMessageDocument> messages = mongoTemplate.find(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            log.debug("查询组织广播消息完成 - 组织ID: {}, 数量: {}", orgId, messages.size());
            return messages;
            
        } catch (Exception e) {
            log.error("查询组织广播消息失败 - 组织ID: {}, 错误: {}", orgId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<BroadcastMessageDocument> findSystemMessages(String messageType) {
        try {
            log.debug("查询系统广播消息 - 类型: {}", messageType);
            
            Criteria criteria = Criteria.where("scope").is("SYSTEM");
            
            if (messageType != null && !messageType.trim().isEmpty()) {
                criteria.and("messageType").is(messageType);
            }
            
            Query query = new Query(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
            
            List<BroadcastMessageDocument> messages = mongoTemplate.find(query, BroadcastMessageDocument.class, COLLECTION_NAME);
            
            log.debug("查询系统广播消息完成 - 数量: {}", messages.size());
            return messages;
            
        } catch (Exception e) {
            log.error("查询系统广播消息失败 - 错误: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 构建用户可见消息查询条件
     */
    private Criteria buildUserVisibleCriteria(Long orgId, String messageType, String scope) {
        Criteria criteria = new Criteria();
        
        // 构建范围条件：系统级消息 或 组织级消息
        Criteria scopeCriteria = new Criteria();
        scopeCriteria.orOperator(
            // 系统级消息：所有用户可见
            Criteria.where("scope").is("SYSTEM"),
            // 组织级消息：用户所属组织可见
            new Criteria().andOperator(
                Criteria.where("scope").is("ORG"),
                new Criteria().orOperator(
                    Criteria.where("oid").is(orgId),
                    Criteria.where("targetOid").in(orgId)
                )
            )
        );
        
        criteria.andOperator(scopeCriteria);
        
        // 添加消息类型筛选
        if (messageType != null && !messageType.trim().isEmpty()) {
            criteria.and("messageType").is(messageType);
        }
        
        // 添加范围筛选
        if (scope != null && !scope.trim().isEmpty()) {
            criteria.and("scope").is(scope);
        }
        
        return criteria;
    }
}
package org.nan.cloud.message.infrastructure.mongodb.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * 广播消息MongoDB Repository (message-service)
 * 
 * 负责在message-service中存储广播消息内容
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BroadcastMessageRepository {

    private final MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "broadcast_message";

    /**
     * 保存广播消息
     * 
     * @param document 广播消息文档
     * @return 保存的文档ID
     */
    public String save(BroadcastMessageDocument document) {
        try {
            log.debug("保存广播消息 - 消息ID: {}, 范围: {}", document.getMessageId(), document.getScope());
            
            BroadcastMessageDocument saved = mongoTemplate.save(document, COLLECTION_NAME);
            
            log.info("保存广播消息成功 - 消息ID: {}, MongoDB ID: {}", 
                    document.getMessageId(), saved.getId());
            
            return saved.getId();
            
        } catch (Exception e) {
            log.error("保存广播消息失败 - 消息ID: {}, 错误: {}", 
                    document.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("保存广播消息失败", e);
        }
    }

    /**
     * 根据消息ID查询广播消息
     * 
     * @param messageId 消息ID
     * @return 广播消息文档
     */
    public BroadcastMessageDocument findByMessageId(String messageId) {
        try {
            log.debug("根据消息ID查询广播消息 - 消息ID: {}", messageId);
            
            BroadcastMessageDocument document = mongoTemplate.findOne(
                org.springframework.data.mongodb.core.query.Query.query(
                    org.springframework.data.mongodb.core.query.Criteria.where("messageId").is(messageId)
                ), 
                BroadcastMessageDocument.class, 
                COLLECTION_NAME
            );
            
            if (document != null) {
                log.debug("根据消息ID查询广播消息成功 - 消息ID: {}", messageId);
            } else {
                log.debug("根据消息ID查询广播消息为空 - 消息ID: {}", messageId);
            }
            
            return document;
            
        } catch (Exception e) {
            log.error("根据消息ID查询广播消息失败 - 消息ID: {}, 错误: {}", messageId, e.getMessage(), e);
            return null;
        }
    }
}
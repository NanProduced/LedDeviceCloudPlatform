package org.nan.cloud.message.infrastructure.mongodb.repository;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 因为message-service没有什么核心业务
 * 只做三方的存储、执行
 * 所以这里不实现接口，直接完成存储逻辑
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RealtimeMessageRepository {

    private final MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "realtime_message";

    /**
     * 保存实时消息
     * @param message
     * @return
     */
    public String save(RealtimeMessageDocument message) {
        try {
            if (message.getCreateTime() == null) {
                message.setCreateTime(LocalDateTime.now());
            }

            RealtimeMessageDocument saved = mongoTemplate.save(message, COLLECTION_NAME);
            log.debug("✅ 实时消息保存成功 - ID: {}, 用户: {}, 类型: {}",
                    saved.getId(), saved.getUid(), saved.getMessageType());
            return saved.getId();
        } catch (Exception e) {
            log.error("❌ 保存实时消息失败 - 用户: {}, 类型: {}, 错误: {}",
                    message.getUid(), message.getMessageType(), e.getMessage(), e);
            throw new RuntimeException("保存实时消息失败", e);
        }
    }

    public boolean batchSave(List<RealtimeMessageDocument> messages) {
        try {
            messages.forEach(msg -> {
                if (msg.getCreateTime() == null) {
                    msg.setCreateTime(LocalDateTime.now());
                }
            });

            mongoTemplate.insertAll(messages);
            log.info("✅ 批量保存实时消息成功 - 数量: {}", messages.size());
            return true;
        } catch (Exception e) {
            log.error("❌ 批量保存实时消息失败 - 数量: {}, 错误: {}", messages.size(), e.getMessage(), e);
            return false;
        }
    }

    public boolean updateReadStatus(String messageId, Long userId, String readAt) {
        try {
            Query query = new Query(Criteria.where("messageId").is(messageId).and("uid").is(userId));
            Update update = new Update()
                    .set("isRead", true)
                    .set("readAt", readAt);

            UpdateResult result = mongoTemplate.updateFirst(query, update, RealtimeMessageDocument.class, COLLECTION_NAME);

            boolean success = result.getModifiedCount() > 0;
            log.debug("✅ 更新消息已读状态 - 消息ID: {}, 用户: {}, 成功: {}", messageId, userId, success);
            return success;
        } catch (Exception e) {
            log.error("❌ 更新消息已读状态失败 - 消息ID: {}, 用户: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            return false;
        }
    }


}

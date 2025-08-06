package org.nan.cloud.message.infrastructure.mongodb.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageReadStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 广播消息已读状态MongoDB Repository (message-service)
 * 
 * 负责在message-service中存储广播消息已读状态
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class BroadcastMessageReadStatusRepository {

    private final MongoTemplate mongoTemplate;

    private static final String COLLECTION_NAME = "broadcast_message_read_status";

    /**
     * 保存已读状态（upsert模式）
     * 
     * @param readStatus 已读状态记录
     * @return 保存的记录ID
     */
    public String save(BroadcastMessageReadStatus readStatus) {
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
            throw new RuntimeException("保存广播消息已读状态失败", e);
        }
    }

    /**
     * 批量保存已读状态
     * 
     * @param readStatusList 已读状态记录列表
     * @return 保存成功的记录数量
     */
    public int batchSave(List<BroadcastMessageReadStatus> readStatusList) {
        if (readStatusList == null || readStatusList.isEmpty()) {
            return 0;
        }

        try {
            log.debug("批量保存广播消息已读状态 - 数量: {}", readStatusList.size());
            
            int successCount = 0;
            for (BroadcastMessageReadStatus readStatus : readStatusList) {
                try {
                    save(readStatus);
                    successCount++;
                } catch (Exception e) {
                    log.warn("批量保存中单个记录失败 - 消息ID: {}, 用户ID: {}, 错误: {}", 
                            readStatus.getMessageId(), readStatus.getUserId(), e.getMessage());
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

    /**
     * 根据ID查询已读状态
     * 
     * @param id 复合ID (messageId_userId)
     * @return 已读状态记录
     */
    public BroadcastMessageReadStatus findById(String id) {
        try {
            log.debug("根据ID查询广播消息已读状态 - ID: {}", id);
            
            BroadcastMessageReadStatus readStatus = mongoTemplate.findById(id, BroadcastMessageReadStatus.class, COLLECTION_NAME);
            
            if (readStatus != null) {
                log.debug("根据ID查询广播消息已读状态成功 - ID: {}", id);
            } else {
                log.debug("根据ID查询广播消息已读状态为空 - ID: {}", id);
            }
            
            return readStatus;
            
        } catch (Exception e) {
            log.error("根据ID查询广播消息已读状态失败 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return null;
        }
    }
}
package org.nan.cloud.message.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.MessageDetailResponse;
import org.nan.cloud.message.api.dto.response.MessageListResponse;
import org.nan.cloud.message.api.event.MessageEvent;
import org.nan.cloud.message.domain.repository.MessagePersistenceRepositoryInterface;
import org.nan.cloud.message.infrastructure.mongodb.document.MessageDetail;
import org.nan.cloud.message.infrastructure.mongodb.repository.MessageDetailMongoRepository;
import org.nan.cloud.message.infrastructure.mysql.entity.MessageInfo;
import org.nan.cloud.message.infrastructure.mysql.mapper.MessageInfoMapper;
import org.nan.cloud.message.infrastructure.redis.manager.MessageCacheManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 消息持久化聚合仓储实现
 * 
 * 实现MessagePersistenceRepositoryInterface接口，
 * 统一协调MySQL、MongoDB和Redis三个数据源的消息数据操作，
 * 为上层业务提供统一的数据访问接口，屏蔽底层存储细节。
 * 
 * 数据分工策略：
 * - MySQL：存储消息元数据、状态信息、统计数据
 * - MongoDB：存储消息详细内容、附件信息、扩展数据
 * - Redis：缓存实时状态、用户在线信息、未读消息计数
 * 
 * 核心功能：
 * - 消息的完整生命周期管理
 * - 多数据源的事务协调
 * - 读写性能优化
 * - 缓存策略管理
 * - 数据一致性保障
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MessagePersistenceRepository implements MessagePersistenceRepositoryInterface {
    
    private final MessageInfoMapper messageInfoMapper;
    private final MessageDetailMongoRepository messageDetailMongoRepository;
    private final MessageCacheManager messageCacheManager;
    
    // ==================== 消息创建和保存 ====================
    
    /**
     * 保存完整消息
     * 
     * 协调多个数据源，确保消息数据的完整性和一致性。
     * 采用先MySQL后MongoDB的保存顺序，确保关联关系正确。
     * 
     * @param event 消息事件对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(MessageEvent event) {
        try {
            log.info("开始保存消息: messageId={}, type={}, receiverId={}", 
                    event.getMessageId(), event.getEventType(), event.getReceiverId());
            
            // 1. 保存到MySQL（消息元数据）
            MessageInfo messageInfo = buildMessageInfo(event);
            messageInfoMapper.insert(messageInfo);
            log.debug("消息元数据保存成功: messageId={}", event.getMessageId());
            
            // 2. 保存到MongoDB（消息详细内容）
            MessageDetail messageDetail = buildMessageDetail(event);
            messageDetailMongoRepository.save(messageDetail);
            log.debug("消息内容保存成功: messageId={}", event.getMessageId());
            
            // 3. 更新Redis缓存
            messageCacheManager.cacheMessageStatus(event.getMessageId(), "PENDING", 24);
            
            // 4. 更新未读消息计数（如果有接收者）
            if (event.getReceiverId() != null && !event.getReceiverId().isEmpty()) {
                messageCacheManager.incrementUnreadCount(
                    event.getReceiverId(), event.getOrganizationId(), event.getMessageId());
            }
            
            log.info("消息保存完成: messageId={}", event.getMessageId());
            
        } catch (Exception e) {
            log.error("消息保存失败: messageId={}, error={}", 
                     event.getMessageId(), e.getMessage(), e);
            throw new RuntimeException("消息保存失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量保存消息
     * 
     * 优化批量消息的保存性能，采用批处理策略。
     * 
     * @param events 消息事件列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveMessages(List<MessageEvent> events) {
        try {
            log.info("开始批量保存消息: count={}", events.size());
            
            // 1. 批量保存到MySQL
            List<MessageInfo> messageInfos = events.stream()
                .map(this::buildMessageInfo)
                .toList();
            
            for (MessageInfo info : messageInfos) {
                messageInfoMapper.insert(info);
            }
            log.debug("批量保存消息元数据完成: count={}", messageInfos.size());
            
            // 2. 批量保存到MongoDB
            List<MessageDetail> messageDetails = events.stream()
                .map(this::buildMessageDetail)
                .collect(Collectors.toList());
            
            messageDetailMongoRepository.saveAll(messageDetails);
            log.debug("批量保存消息内容完成: count={}", messageDetails.size());
            
            // 3. 批量更新缓存
            for (MessageEvent event : events) {
                messageCacheManager.cacheMessageStatus(event.getMessageId(), "PENDING", 24);
                
                if (event.getReceiverId() != null && !event.getReceiverId().isEmpty()) {
                    messageCacheManager.incrementUnreadCount(
                        event.getReceiverId(), event.getOrganizationId(), event.getMessageId());
                }
            }
            
            log.info("批量保存消息完成: count={}", events.size());
            
        } catch (Exception e) {
            log.error("批量保存消息失败: count={}, error={}", events.size(), e.getMessage(), e);
            throw new RuntimeException("批量保存消息失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 消息查询 ====================
    
    /**
     * 获取完整消息信息
     * 
     * 关联查询MySQL和MongoDB，返回完整的消息信息。
     * 优先从缓存获取状态信息，提高查询性能。
     * 
     * @param messageId 消息ID
     * @return 完整消息信息
     */
    public Optional<MessageDetailResponse> getCompleteMessage(String messageId) {
        try {
            // 1. 从MySQL获取元数据
            MessageInfo messageInfo = messageInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageInfo>()
                    .eq(MessageInfo::getMessageId, messageId)
            );
            
            if (messageInfo == null) {
                log.debug("消息元数据不存在: messageId={}", messageId);
                return Optional.empty();
            }
            
            // 2. 从MongoDB获取详细内容
            Optional<MessageDetail> messageDetailOpt = messageDetailMongoRepository.findByMessageId(messageId);
            if (messageDetailOpt.isEmpty()) {
                log.warn("消息详细内容不存在: messageId={}", messageId);
                return Optional.empty();
            }
            
            // 3. 从缓存获取最新状态
            String cachedStatus = messageCacheManager.getMessageStatus(messageId);
            if (cachedStatus != null) {
                messageInfo.setStatus(cachedStatus);
            }
            
            // 4. 构建完整响应
            MessageDetailResponse response = buildMessageDetailResponse(messageInfo, messageDetailOpt.get());
            
            log.debug("获取完整消息成功: messageId={}", messageId);
            return Optional.of(response);
            
        } catch (Exception e) {
            log.error("获取完整消息失败: messageId={}, error={}", messageId, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 分页查询用户消息
     * 
     * 先从MySQL查询元数据（支持高效分页和筛选），
     * 再从MongoDB批量获取详细内容，最后合并结果。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param messageType 消息类型（可选）
     * @param status 消息状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页消息列表
     */
    public MessageListResponse getMessagesByPage(String receiverId, String organizationId, 
                                               String messageType, String status, 
                                               int page, int size) {
        try {
            log.debug("分页查询用户消息: receiverId={}, organizationId={}, page={}, size={}", 
                     receiverId, organizationId, page, size);
            
            // 1. 从MySQL分页查询元数据
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<MessageInfo> mysqlPage = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageInfo> queryWrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageInfo>()
                    .eq(MessageInfo::getReceiverId, receiverId)
                    .eq(MessageInfo::getOrganizationId, organizationId)
                    .orderByDesc(MessageInfo::getCreatedTime);
            
            if (messageType != null && !messageType.isEmpty()) {
                queryWrapper.eq(MessageInfo::getMessageType, messageType);
            }
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq(MessageInfo::getStatus, status);
            }
            
            com.baomidou.mybatisplus.core.metadata.IPage<MessageInfo> messageInfoPage = 
                messageInfoMapper.selectPage(mysqlPage, queryWrapper);
            
            // 2. 批量从MongoDB获取详细内容
            List<String> messageIds = messageInfoPage.getRecords().stream()
                .map(MessageInfo::getMessageId)
                .collect(Collectors.toList());
            
            Map<String, MessageDetail> detailMap = messageDetailMongoRepository
                .findByMessageIdInOrderByCreatedTimeDesc(messageIds)
                .stream()
                .collect(Collectors.toMap(MessageDetail::getMessageId, detail -> detail));
            
            // 3. 从缓存获取最新状态
            Map<String, String> statusMap = messageCacheManager.batchGetMessageStatus(messageIds);
            
            // 4. 合并数据构建响应
            List<MessageDetailResponse> messageDetails = messageInfoPage.getRecords().stream()
                .map(info -> {
                    // 更新缓存中的状态
                    String cachedStatus = statusMap.get(info.getMessageId());
                    if (cachedStatus != null) {
                        info.setStatus(cachedStatus);
                    }
                    
                    MessageDetail detail = detailMap.get(info.getMessageId());
                    return buildMessageDetailResponse(info, detail);
                })
                .collect(Collectors.toList());
            
            MessageListResponse response = MessageListResponse.builder()
                .messages(messageDetails)
                .total(messageInfoPage.getTotal())
                .page(page)
                .size(size)
                .build();
            
            log.debug("分页查询用户消息完成: receiverId={}, total={}", receiverId, response.getTotal());
            return response;
            
        } catch (Exception e) {
            log.error("分页查询用户消息失败: receiverId={}, error={}", receiverId, e.getMessage(), e);
            return MessageListResponse.builder()
                .messages(List.of())
                .total(0L)
                .page(page)
                .size(size)
                .build();
        }
    }
    
    /**
     * 查询用户未读消息
     * 
     * 优先从缓存获取未读消息列表，提高查询性能。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param limit 限制数量
     * @return 未读消息列表
     */
    public List<MessageDetailResponse> getUnreadMessages(String receiverId, String organizationId, int limit) {
        try {
            log.debug("查询用户未读消息: receiverId={}, organizationId={}, limit={}", 
                     receiverId, organizationId, limit);
            
            // 1. 从缓存获取最近的未读消息ID
            var unreadMessageIds = messageCacheManager.getRecentUnreadMessages(receiverId, limit);
            
            if (unreadMessageIds.isEmpty()) {
                // 2. 缓存为空时从MySQL查询
                List<MessageInfo> messageInfos = messageInfoMapper.selectRecentUnreadMessages(
                    receiverId, organizationId, limit);
                
                if (messageInfos.isEmpty()) {
                    return List.of();
                }
                
                // 更新缓存
                for (MessageInfo info : messageInfos) {
                    messageCacheManager.incrementUnreadCount(
                        receiverId, organizationId, info.getMessageId());
                }
                
                // 获取详细内容
                List<String> messageIds = messageInfos.stream()
                    .map(MessageInfo::getMessageId)
                    .collect(Collectors.toList());
                
                return buildCompleteMessages(messageInfos, messageIds);
            } else {
                // 3. 从缓存的消息ID获取完整信息
                List<String> messageIds = new ArrayList<>(unreadMessageIds);
                
                // 从MySQL获取元数据
                LambdaQueryWrapper<MessageInfo> queryWrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MessageInfo>()
                        .in(MessageInfo::getMessageId, messageIds)
                        .eq(MessageInfo::getReceiverId, receiverId)
                        .eq(MessageInfo::getOrganizationId, organizationId)
                        .orderByDesc(MessageInfo::getCreatedTime);
                
                List<MessageInfo> messageInfos = messageInfoMapper.selectList(queryWrapper);
                
                return buildCompleteMessages(messageInfos, messageIds);
            }
            
        } catch (Exception e) {
            log.error("查询用户未读消息失败: receiverId={}, error={}", receiverId, e.getMessage(), e);
            return List.of();
        }
    }
    
    // ==================== 消息状态更新 ====================
    
    /**
     * 更新消息状态
     * 
     * 先更新缓存（快速响应），再异步更新MySQL（持久化）。
     * 
     * @param messageId 消息ID
     * @param newStatus 新状态
     */
    public void updateMessageStatus(String messageId, String newStatus) {
        try {
            log.debug("更新消息状态: messageId={}, newStatus={}", messageId, newStatus);
            
            // 1. 立即更新缓存
            messageCacheManager.cacheMessageStatus(messageId, newStatus, 24);
            
            // 2. 异步更新MySQL
            LocalDateTime updateTime = LocalDateTime.now();
            int updated = messageInfoMapper.batchUpdateStatus(
                List.of(messageId), newStatus, updateTime);
            
            if (updated > 0) {
                log.debug("消息状态更新成功: messageId={}, newStatus={}", messageId, newStatus);
            } else {
                log.warn("消息状态更新无影响: messageId={}, newStatus={}", messageId, newStatus);
            }
            
        } catch (Exception e) {
            log.error("更新消息状态失败: messageId={}, newStatus={}, error={}", 
                     messageId, newStatus, e.getMessage(), e);
        }
    }
    
    /**
     * 批量标记消息为已读
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @param messageIds 消息ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void markMessagesAsRead(String receiverId, String organizationId, List<String> messageIds) {
        try {
            log.info("批量标记消息已读: receiverId={}, organizationId={}, count={}", 
                    receiverId, organizationId, messageIds.size());
            
            // 1. 批量更新MySQL状态
            LocalDateTime readTime = LocalDateTime.now();
            int updated = messageInfoMapper.batchUpdateStatus(messageIds, "READ", readTime);
            
            // 2. 批量更新缓存状态
            for (String messageId : messageIds) {
                messageCacheManager.cacheMessageStatus(messageId, "READ", 24);
            }
            
            // 3. 更新未读消息计数
            messageCacheManager.markMessagesAsRead(receiverId, organizationId, messageIds);
            
            log.info("批量标记消息已读完成: receiverId={}, updated={}", receiverId, updated);
            
        } catch (Exception e) {
            log.error("批量标记消息已读失败: receiverId={}, error={}", receiverId, e.getMessage(), e);
            throw new RuntimeException("批量标记消息已读失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户未读消息数量
     * 
     * 优先从缓存获取，缓存不存在时从MySQL查询并更新缓存。
     * 
     * @param receiverId 接收者用户ID
     * @param organizationId 组织ID
     * @return 未读消息数量
     */
    public Long getUnreadMessageCount(String receiverId, String organizationId) {
        try {
            // 1. 先从缓存获取
            Long cachedCount = messageCacheManager.getUnreadCount(receiverId, organizationId);
            if (cachedCount != null && cachedCount > 0) {
                return cachedCount;
            }
            
            // 2. 缓存不存在时从MySQL查询
            Long mysqlCount = messageInfoMapper.countUnreadMessages(receiverId, organizationId);
            
            // 3. 更新缓存（如果有未读消息）
            if (mysqlCount > 0) {
                // 获取未读消息列表并更新缓存
                List<MessageInfo> unreadMessages = messageInfoMapper
                    .selectRecentUnreadMessages(receiverId, organizationId, mysqlCount.intValue());
                
                for (MessageInfo message : unreadMessages) {
                    messageCacheManager.incrementUnreadCount(
                        receiverId, organizationId, message.getMessageId());
                }
            }
            
            return mysqlCount;
            
        } catch (Exception e) {
            log.error("获取未读消息数量失败: receiverId={}, organizationId={}, error={}", 
                     receiverId, organizationId, e.getMessage(), e);
            return 0L;
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建MySQL消息实体
     */
    private MessageInfo buildMessageInfo(MessageEvent event) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setMessageId(event.getMessageId());
        messageInfo.setMessageType(event.getEventType());
        messageInfo.setSenderId(event.getSenderId());
        messageInfo.setReceiverId(event.getReceiverId());
        messageInfo.setOrganizationId(event.getOrganizationId());
        messageInfo.setTitle(event.getTitle());
        messageInfo.setPriority(event.getPriority() != null ? event.getPriority().getLevel() : 2);
        messageInfo.setStatus("PENDING");
        messageInfo.setCreatedTime(event.getTimestamp());
        
        // 设置消息过期时间（默认30天）
        if (event.getExpireTime() != null) {
            messageInfo.setExpiresAt(event.getExpireTime());
        } else {
            messageInfo.setExpiresAt(LocalDateTime.now().plusDays(30));
        }
        
        return messageInfo;
    }
    
    /**
     * 构建MongoDB消息详情文档
     */
    private MessageDetail buildMessageDetail(MessageEvent event) {
        return MessageDetail.builder()
            .messageId(event.getMessageId())
            .organizationId(event.getOrganizationId())
            .messageType(event.getEventType())
            .senderId(event.getSenderId())
            .senderName(event.getSenderName())
            .receiverId(event.getReceiverId())
            .title(event.getTitle())
            .content(event.getContent())
            .metadata(event.getMetadata())
            .priority(event.getPriority() != null ? event.getPriority().getLevel() : 2)
            .source("message-service")
            .createdTime(event.getTimestamp())
            .ttl(LocalDateTime.now().plus(30, ChronoUnit.DAYS)) // TTL 30天
            .build();
    }
    
    /**
     * 构建完整消息响应
     */
    private MessageDetailResponse buildMessageDetailResponse(MessageInfo info, MessageDetail detail) {
        return MessageDetailResponse.builder()
            .messageId(info.getMessageId())
            .messageType(info.getMessageType())
            .senderId(info.getSenderId())
            .senderName(detail != null ? detail.getSenderName() : null)
            .receiverId(info.getReceiverId())
            .organizationId(info.getOrganizationId())
            .title(info.getTitle())
            .content(detail != null ? detail.getContent() : null)
            .metadata(detail != null ? detail.getMetadata() : null)
            .tags(detail != null ? detail.getTags() : null)
            .priority(info.getPriority())
            .status(info.getStatus())
            .createdTime(info.getCreatedTime())
            .expireTime(info.getExpiresAt())
            .build();
    }
    
    /**
     * 构建完整消息列表
     */
    private List<MessageDetailResponse> buildCompleteMessages(List<MessageInfo> messageInfos, List<String> messageIds) {
        if (messageInfos.isEmpty()) {
            return List.of();
        }
        
        // 获取MongoDB详细内容
        Map<String, MessageDetail> detailMap = messageDetailMongoRepository
            .findByMessageIdInOrderByCreatedTimeDesc(messageIds)
            .stream()
            .collect(Collectors.toMap(MessageDetail::getMessageId, detail -> detail));
        
        // 获取缓存状态
        Map<String, String> statusMap = messageCacheManager.batchGetMessageStatus(messageIds);
        
        // 构建响应
        return messageInfos.stream()
            .map(info -> {
                String cachedStatus = statusMap.get(info.getMessageId());
                if (cachedStatus != null) {
                    info.setStatus(cachedStatus);
                }
                
                MessageDetail detail = detailMap.get(info.getMessageId());
                return buildMessageDetailResponse(info, detail);
            })
            .collect(Collectors.toList());
    }
}
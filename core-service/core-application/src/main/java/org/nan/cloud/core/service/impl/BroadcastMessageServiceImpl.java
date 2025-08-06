package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.BroadcastMessageDocument;
import org.nan.cloud.common.basic.domain.BroadcastMessageReadStatus;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryBroadcastMessageRequest;
import org.nan.cloud.core.api.DTO.res.BroadcastMessageResponse;
import org.nan.cloud.core.repository.BroadcastMessageRepository;
import org.nan.cloud.core.repository.BroadcastMessageReadStatusRepository;
import org.nan.cloud.core.service.BroadcastMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 广播消息服务实现类
 * 
 * 基于分离存储架构：
 * - BroadcastMessageDocument：消息内容（一条消息一条记录）
 * - BroadcastMessageReadStatus：用户已读状态（用户读了才创建记录）
 * 
 * 查询策略：分步查询 + 内存合并
 * 注：通知消息不常用，不需要缓存
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastMessageServiceImpl implements BroadcastMessageService {
    
    private final BroadcastMessageRepository broadcastMessageRepository;
    private final BroadcastMessageReadStatusRepository readStatusRepository;
    
    @Override
    public PageVO<BroadcastMessageResponse> getUserBroadcastMessages(
        PageRequestDTO<QueryBroadcastMessageRequest> pageRequest, 
        Long userId, Long orgId) {
        
        try {
            log.debug("查询用户广播消息 - 用户ID: {}, 组织ID: {}, 页码: {}, 大小: {}", 
                    userId, orgId, pageRequest.getPageNum(), pageRequest.getPageSize());
            
            QueryBroadcastMessageRequest query = pageRequest.getParams();
            
            // Step 1: 查询用户可见的广播消息（分页）
            PageVO<BroadcastMessageDocument> messagePage = broadcastMessageRepository
                .findUserVisibleMessages(
                    pageRequest.getPageNum(), 
                    pageRequest.getPageSize(), 
                    query != null ? query.getMessageType() : null,
                    query != null ? query.getScope() : null,
                    orgId);
            
            if (messagePage.getRecords().isEmpty()) {
                return PageVO.<BroadcastMessageResponse>builder()
                        .records(Collections.emptyList())
                        .total(0L)
                        .pageNum(pageRequest.getPageNum())
                        .pageSize(pageRequest.getPageSize())
                        .totalPages(0)
                        .build();
            }
            
            // Step 2: 批量查询用户已读状态
            List<String> messageIds = messagePage.getRecords().stream()
                    .map(BroadcastMessageDocument::getMessageId)
                    .collect(Collectors.toList());
            
            Set<String> readMessageIds = readStatusRepository.findReadMessageIds(userId, messageIds);
            
            // Step 3: 合并结果并构造响应对象
            List<BroadcastMessageResponse> responses = messagePage.getRecords().stream()
                    .map(msg -> convertToResponse(msg, readMessageIds.contains(msg.getMessageId())))
                    .collect(Collectors.toList());
            
            // 如果只查未读消息，需要过滤
            if (query != null && Boolean.TRUE.equals(query.getOnlyUnread())) {
                responses = responses.stream()
                        .filter(response -> !response.getIsRead())
                        .collect(Collectors.toList());
            }
            
            PageVO<BroadcastMessageResponse> result = messagePage.withRecords(responses);
            
            log.debug("查询用户广播消息完成 - 用户ID: {}, 返回数量: {}/{}", 
                    userId, responses.size(), messagePage.getTotal());
            
            return result;
            
        } catch (Exception e) {
            log.error("查询用户广播消息失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "查询广播消息失败");
        }
    }
    
    @Override
    public Long getUnreadCount(Long userId, Long orgId) {
        try {
            long count = readStatusRepository.countUnreadMessages(userId, orgId);
            log.debug("查询用户未读广播消息数量 - 用户ID: {}, 数量: {}", userId, count);
            return count;
            
        } catch (Exception e) {
            log.error("查询用户未读广播消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0L; // 异常时返回0，避免影响前端显示
        }
    }
    
    @Override
    public Map<String, Long> getUnreadCountByType(Long userId, Long orgId) {
        try {
            Map<String, Long> countMap = readStatusRepository.countUnreadMessagesByType(userId, orgId);
            if (countMap == null) {
                countMap = Collections.emptyMap();
            }
            
            log.debug("查询用户按类型未读广播消息数量 - 用户ID: {}, 结果: {}", userId, countMap);
            return countMap;
            
        } catch (Exception e) {
            log.error("查询用户按类型未读广播消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public List<BroadcastMessageResponse> getUnreadNotificationsForLogin(Long userId, Long orgId, int limit) {
        try {
            log.debug("查询用户登录时推送的未读通知 - 用户ID: {}, 组织ID: {}, 限制: {}", userId, orgId, limit);
            
            // 查询未读的通知消息（按优先级和时间排序）
            List<BroadcastMessageDocument> unreadMessages = broadcastMessageRepository
                .findUnreadNotificationsForUser(userId, orgId, limit);
            
            if (unreadMessages.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 转换为响应对象（这些消息都是未读的）
            List<BroadcastMessageResponse> responses = unreadMessages.stream()
                    .map(msg -> convertToResponse(msg, false)) // 未读状态
                    .collect(Collectors.toList());
            
            log.debug("查询登录推送未读通知完成 - 用户ID: {}, 数量: {}", userId, responses.size());
            return responses;
            
        } catch (Exception e) {
            log.error("查询登录推送未读通知失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyList(); // 异常时返回空列表，不影响登录流程
        }
    }
    
    @Override
    public BroadcastMessageResponse getMessageById(String messageId, Long userId) {
        try {
            log.debug("根据消息ID查询广播消息详情 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            // 查询消息内容
            BroadcastMessageDocument message = broadcastMessageRepository.findByMessageId(messageId)
                    .orElseThrow(() -> new BaseException(ExceptionEnum.DETAILS_DENIED, "消息不存在"));
            
            // 查询用户已读状态
            boolean isRead = readStatusRepository.isMessageRead(userId, messageId);
            
            return convertToResponse(message, isRead);
            
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询广播消息详情失败 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "查询消息详情失败");
        }
    }
    
    @Override
    public void markMessageAsRead(String messageId, Long userId, Long orgId) {
        try {
            log.debug("标记广播消息已读 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
            // 构建已读状态记录
            BroadcastMessageReadStatus readStatus = BroadcastMessageReadStatus.builder()
                .id(buildReadStatusId(messageId, userId))
                .messageId(messageId)
                .userId(userId)
                .orgId(orgId)
                .readAt(LocalDateTime.now())
                .build();
            
            // 保存已读状态（upsert模式）
            readStatusRepository.upsert(readStatus);
            
            log.info("广播消息标记已读成功 - 消息ID: {}, 用户ID: {}", messageId, userId);
            
        } catch (Exception e) {
            log.error("标记广播消息已读失败 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            // 不抛异常，避免影响用户体验
        }
    }
    
    @Override
    public void batchMarkMessagesAsRead(List<String> messageIds, Long userId, Long orgId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        try {
            log.debug("批量标记广播消息已读 - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
            
            List<BroadcastMessageReadStatus> statusList = messageIds.stream()
                    .map(messageId -> BroadcastMessageReadStatus.builder()
                        .id(buildReadStatusId(messageId, userId))
                        .messageId(messageId)
                        .userId(userId)
                        .orgId(orgId)
                        .readAt(LocalDateTime.now())
                        .build())
                    .collect(Collectors.toList());
            
            // 批量保存已读状态
            readStatusRepository.batchUpsert(statusList);
            
            log.info("批量标记广播消息已读成功 - 用户ID: {}, 消息数量: {}", userId, messageIds.size());
            
        } catch (Exception e) {
            log.error("批量标记广播消息已读失败 - 用户ID: {}, 消息数量: {}, 错误: {}", 
                    userId, messageIds.size(), e.getMessage(), e);
        }
    }
    
    @Override
    public void clearUnreadCountCache(Long userId) {
        // 通知消息不需要缓存，此方法为空实现
        log.debug("广播消息不使用缓存，跳过清理操作 - 用户ID: {}", userId);
    }
    
    @Override
    public void batchClearUnreadCountCache(List<Long> userIds) {
        // 通知消息不需要缓存，此方法为空实现
        log.debug("广播消息不使用缓存，跳过批量清理操作 - 用户数量: {}", 
                userIds != null ? userIds.size() : 0);
    }
    
    /**
     * 内部方法：转换文档对象为响应对象
     */
    private BroadcastMessageResponse convertToResponse(BroadcastMessageDocument document, boolean isRead) {
        return BroadcastMessageResponse.builder()
                .id(document.getId())
                .messageId(document.getMessageId())
                .timestamp(document.getTimestamp())
                .oid(document.getOid())
                .messageType(document.getMessageType())
                .subType_1(document.getSubType_1())
                .subType_2(document.getSubType_2())
                .level(document.getLevel())
                .scope(document.getScope())
                .targetOid(document.getTargetOid())
                .title(document.getTitle())
                .content(document.getContent())
                .payload(document.getPayload())
                .expiredAt(document.getExpiredAt())
                .publisherId(document.getPublisherId())
                .isRead(isRead)
                .readAt(null) // 暂时不返回具体已读时间
                .build();
    }
    
    /**
     * 内部方法：构建已读状态复合ID
     */
    private String buildReadStatusId(String messageId, Long userId) {
        return messageId + "_" + userId;
    }
    
}
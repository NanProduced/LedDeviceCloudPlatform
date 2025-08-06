package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.domain.RealtimeMessageDocument;
import org.nan.cloud.common.basic.exception.BaseException;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.model.PageRequestDTO;
import org.nan.cloud.common.basic.model.PageVO;
import org.nan.cloud.core.api.DTO.req.QueryRealtimeMessageRequest;
import org.nan.cloud.core.api.DTO.res.RealtimeMessageResponse;
import org.nan.cloud.core.enums.CacheType;
import org.nan.cloud.core.repository.RealtimeMessageRepository;
import org.nan.cloud.core.service.CacheService;
import org.nan.cloud.core.service.RealtimeMessageService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实时消息服务实现类
 * 
 * 提供实时消息查询、统计和缓存管理功能
 * 符合CacheServiceImpl的缓存使用规范
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeMessageServiceImpl implements RealtimeMessageService {
    
    private final RealtimeMessageRepository realtimeMessageRepository;
    private final CacheService cacheService;
    
    // 缓存Key常量
    private static final String UNREAD_COUNT_CACHE_KEY = "realtime:unread:count:%d"; // userId
    private static final String UNREAD_COUNT_BY_TYPE_CACHE_KEY = "realtime:unread:type:%d"; // userId
    
    // 缓存配置
    private static final CacheType UNREAD_COUNT_CACHE_TYPE = CacheType.REALTIME_MESSAGE_UNREAD_COUNT;
    private static final Duration UNREAD_COUNT_CACHE_TTL = Duration.ofMinutes(15);
    
    @Override
    public PageVO<RealtimeMessageResponse> getUserMessages(PageRequestDTO<QueryRealtimeMessageRequest> pageRequest, 
                                                          Long userId, Long orgId) {
        try {
            log.debug("查询用户实时消息 - 用户ID: {}, 组织ID: {}, 页码: {}, 大小: {}", 
                    userId, orgId, pageRequest.getPageNum(), pageRequest.getPageSize());
            
            QueryRealtimeMessageRequest query = pageRequest.getParams();
            String messageType = query != null ? query.getMessageType() : null;
            boolean onlyUnread = query != null ? query.getOnlyUnread() : false;
            
            // 使用Repository标准接口，避免Spring Data依赖
            PageVO<RealtimeMessageDocument> messagePage = realtimeMessageRepository.findUserMessages(
                pageRequest.getPageNum(), 
                pageRequest.getPageSize(), 
                messageType, onlyUnread,
                orgId, 
                userId);
            
            // 转换为响应对象
            List<RealtimeMessageResponse> responses = messagePage.getRecords().stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            PageVO<RealtimeMessageResponse> realtimeMessageResponsePageVO = messagePage.withRecords(responses);

            log.debug("查询用户实时消息完成 - 用户ID: {}, 返回数量: {}/{}", 
                    userId, responses.size(), messagePage.getTotal());
            
            return realtimeMessageResponsePageVO;
            
        } catch (Exception e) {
            log.error("查询用户实时消息失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "查询实时消息失败");
        }
    }
    
    @Override
    public Long getUnreadCount(Long userId, Long orgId) {
        String cacheKey = String.format(UNREAD_COUNT_CACHE_KEY, userId);
        
        try {
            // 使用CacheService的标准缓存模式
            return cacheService.getWithCacheTypeConfig(cacheKey, UNREAD_COUNT_CACHE_TYPE, Long.class);
        } catch (Exception e) {
            log.warn("从缓存获取未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
        
        // 缓存未命中或异常，从数据库查询
        try {
            long count = realtimeMessageRepository.countUnreadMessages(orgId, userId);
            
            // 异步写入缓存
            try {
                cacheService.putWithCacheTypeConfig(cacheKey, count, UNREAD_COUNT_CACHE_TYPE, UNREAD_COUNT_CACHE_TTL);
            } catch (Exception cacheException) {
                log.warn("写入未读消息数量缓存失败 - 用户ID: {}, 错误: {}", userId, cacheException.getMessage());
            }
            
            log.debug("查询用户未读消息数量 - 用户ID: {}, 数量: {}", userId, count);
            return count;
            
        } catch (Exception e) {
            log.error("查询用户未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return 0L; // 异常时返回0，避免影响前端显示
        }
    }
    
    @Override
    public Map<String, Long> getUnreadCountByType(Long userId, Long orgId) {
        String cacheKey = String.format(UNREAD_COUNT_BY_TYPE_CACHE_KEY, userId);
        
        try {
            // 使用CacheService的标准缓存模式
            @SuppressWarnings("unchecked")
            Map<String, Long> cachedResult = (Map<String, Long>) cacheService.getWithCacheTypeConfig(
                cacheKey, UNREAD_COUNT_CACHE_TYPE, Map.class);
            if (cachedResult != null) {
                return cachedResult;
            }
        } catch (Exception e) {
            log.warn("从缓存获取按类型分组未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
        
        // 缓存未命中或异常，从数据库查询
        try {
            Map<String, Long> countMap = realtimeMessageRepository.countUnreadMessagesByType(orgId, userId);
            if (countMap == null) {
                countMap = Collections.emptyMap();
            }
            
            // 异步写入缓存
            try {
                cacheService.putWithCacheTypeConfig(cacheKey, countMap, UNREAD_COUNT_CACHE_TYPE, UNREAD_COUNT_CACHE_TTL);
            } catch (Exception cacheException) {
                log.warn("写入按类型分组未读消息数量缓存失败 - 用户ID: {}, 错误: {}", userId, cacheException.getMessage());
            }
            
            log.debug("查询用户按类型未读消息数量 - 用户ID: {}, 结果: {}", userId, countMap);
            return countMap;
            
        } catch (Exception e) {
            log.error("查询用户按类型未读消息数量失败 - 用户ID: {}, 错误: {}", userId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    @Override
    public RealtimeMessageDocument getMessageById(String messageId, Long userId) {
        try {
            log.debug("根据消息ID查询消息详情 - 消息ID: {}, 用户ID: {}", messageId, userId);

            return realtimeMessageRepository.findByMessageIdAndUserId(messageId, userId)
                    .orElseThrow(() -> new BaseException(ExceptionEnum.DETAILS_DENIED, "消息不存在或无权限访问"));
                    
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询消息详情失败 - 消息ID: {}, 用户ID: {}, 错误: {}", messageId, userId, e.getMessage(), e);
            throw new BaseException(ExceptionEnum.SERVER_ERROR, "查询消息详情失败");
        }
    }
    
    @Override
    public void clearUnreadCountCache(Long userId) {
        try {
            String countCacheKey = String.format(UNREAD_COUNT_CACHE_KEY, userId);
            String typeCacheKey = String.format(UNREAD_COUNT_BY_TYPE_CACHE_KEY, userId);
            
            cacheService.evict(countCacheKey);
            cacheService.evict(typeCacheKey);
            
            log.debug("清除用户未读消息缓存 - 用户ID: {}", userId);
            
        } catch (Exception e) {
            log.warn("清除用户未读消息缓存失败 - 用户ID: {}, 错误: {}", userId, e.getMessage());
        }
    }
    
    @Override
    public void batchClearUnreadCountCache(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        try {
            List<String> cacheKeys = userIds.stream()
                    .flatMap(userId -> java.util.stream.Stream.of(
                        String.format(UNREAD_COUNT_CACHE_KEY, userId),
                        String.format(UNREAD_COUNT_BY_TYPE_CACHE_KEY, userId)
                    ))
                    .collect(Collectors.toList());
            
            cacheService.multiEvict(cacheKeys);
            
            log.debug("批量清除用户未读消息缓存 - 用户数量: {}", userIds.size());
            
        } catch (Exception e) {
            log.warn("批量清除用户未读消息缓存失败 - 用户数量: {}, 错误: {}", userIds.size(), e.getMessage());
        }
    }
    
    /**
     * 转换文档对象为响应对象
     */
    private RealtimeMessageResponse convertToResponse(RealtimeMessageDocument document) {
        return RealtimeMessageResponse.builder()
                .id(document.getId())
                .timestamp(document.getTimestamp())
                .oid(document.getOid())
                .uid(document.getUid())
                .messageType(document.getMessageType())
                .subType_1(document.getSubType_1())
                .subType_2(document.getSubType_2())
                .level(document.getLevel())
                .title(document.getTitle())
                .content(document.getContent())
                .payload(document.getPayload())
                .build();
    }
}

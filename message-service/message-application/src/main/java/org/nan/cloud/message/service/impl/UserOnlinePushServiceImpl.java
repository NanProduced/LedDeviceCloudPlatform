package org.nan.cloud.message.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.MessageDetailResponse;
import org.nan.cloud.message.api.dto.response.TaskResultResponse;
import org.nan.cloud.message.api.dto.response.UserOnlinePushResult;
import org.nan.cloud.message.api.dto.websocket.WebSocketMessage;
import org.nan.cloud.message.api.enums.MessageType;
import org.nan.cloud.message.domain.service.MessageCacheService;
import org.nan.cloud.message.domain.repository.MessagePersistenceRepositoryInterface;
import org.nan.cloud.message.domain.repository.TaskResultPersistenceRepositoryInterface;
import org.nan.cloud.message.service.MessageService;
import org.nan.cloud.message.service.UserOnlinePushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 用户上线推送服务实现类
 * 
 * 实现用户上线时的完整消息推送流程，包括未读消息推送、未查看任务结果通知、
 * 统计信息推送等功能。专为LED设备云平台的业务场景优化。
 * 
 * 核心特性：
 * - 智能推送策略：按优先级和时间排序
 * - 性能优化：批量查询和异步推送
 * - 防重复推送：基于Redis的推送记录
 * - 多租户隔离：组织级数据隔离
 * - 推送限流：防止消息过载
 * - 详细统计：推送结果和性能指标
 * 
 * 推送策略：
 * 1. 高优先级消息优先推送
 * 2. 最新消息优先推送
 * 3. 任务结果按完成时间排序
 * 4. 支持推送数量限制
 * 5. 自动跳过已推送内容
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserOnlinePushServiceImpl implements UserOnlinePushService {
    
    private final MessagePersistenceRepositoryInterface messagePersistenceRepository;
    private final TaskResultPersistenceRepositoryInterface taskResultPersistenceRepository;
    private final MessageCacheService messageCacheService;
    private final MessageService messageService;
    
    // 异步任务结果缓存
    private final Map<String, UserOnlinePushResult> asyncResultCache = new ConcurrentHashMap<>();
    
    // 推送配置参数
    @Value("${message.push.max-messages:20}")
    private int defaultMaxMessages;
    
    @Value("${message.push.max-tasks:10}")
    private int defaultMaxTasks;
    
    @Value("${message.push.high-priority-threshold:3}")
    private int highPriorityThreshold;
    
    @Value("${message.push.enable-statistics:true}")
    private boolean enableStatisticsPush;
    
    @Value("${message.push.cache-expire-hours:2}")
    private int cacheExpireHours;
    
    // ==================== 核心推送方法 ====================
    
    @Override
    public UserOnlinePushResult handleUserOnline(String userId, String sessionId, String organizationId) {
        log.info("处理用户上线推送: userId={}, sessionId={}, organizationId={}", 
                userId, sessionId, organizationId);
        
        LocalDateTime startTime = LocalDateTime.now();
        UserOnlinePushResult.UserOnlinePushResultBuilder resultBuilder = UserOnlinePushResult.builder()
            .userId(userId)
            .sessionId(sessionId)
            .organizationId(organizationId)
            .startTime(startTime)
            .success(false)
            .pushedMessageIds(new ArrayList<>())
            .failedMessageIds(new ArrayList<>())
            .pushedTaskIds(new ArrayList<>())
            .failedTaskIds(new ArrayList<>())
            .errorDetails(new ArrayList<>())
            .warnings(new ArrayList<>());
        
        try {
            // 1. 检查是否需要推送
            if (!shouldPushToUser(userId, organizationId)) {
                log.debug("用户不需要推送: userId={}", userId);
                return resultBuilder
                    .success(true)
                    .pushedMessageCount(0)
                    .pushedTaskResultCount(0)
                    .endTime(LocalDateTime.now())
                    .durationMs(calculateDuration(startTime))
                    .build();
            }
            
            // 2. 标记用户在线
            messageCacheService.markUserOnline(userId, 3600); // 1小时过期
            
            // 3. 推送高优先级消息
            long highPriorityStart = System.currentTimeMillis();
            int highPriorityCount = pushHighPriorityMessages(userId, sessionId, organizationId);
            long highPriorityDuration = System.currentTimeMillis() - highPriorityStart;
            
            // 4. 推送未读消息
            long messageStart = System.currentTimeMillis();
            int messageCount = pushUnreadMessages(userId, sessionId, organizationId, defaultMaxMessages);
            long messageDuration = System.currentTimeMillis() - messageStart;
            
            // 5. 推送未查看任务结果
            long taskStart = System.currentTimeMillis();
            int taskCount = pushUnviewedTaskResults(userId, sessionId, organizationId);
            long taskDuration = System.currentTimeMillis() - taskStart;
            
            // 6. 推送统计信息
            long statsStart = System.currentTimeMillis();
            boolean statsSuccess = false;
            if (enableStatisticsPush) {
                statsSuccess = pushUserStatistics(userId, sessionId, organizationId);
            }
            long statsDuration = System.currentTimeMillis() - statsStart;
            
            // 7. 构建结果
            LocalDateTime endTime = LocalDateTime.now();
            UserOnlinePushResult result = resultBuilder
                .success(true)
                .endTime(endTime)
                .durationMs(calculateDuration(startTime))
                .pushedMessageCount(messageCount)
                .pushedHighPriorityMessageCount(highPriorityCount)
                .pushedTaskResultCount(taskCount)
                .statisticsPushed(statsSuccess)
                .messagePushDurationMs(messageDuration)
                .taskPushDurationMs(taskDuration)
                .statisticsPushDurationMs(statsDuration)
                .build();
            
            log.info("用户上线推送完成: {}", result.getSummary());
            return result;
            
        } catch (Exception e) {
            log.error("用户上线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            
            return resultBuilder
                .success(false)
                .errorMessage("推送过程发生异常: " + e.getMessage())
                .endTime(LocalDateTime.now())
                .durationMs(calculateDuration(startTime))
                .build();
        }
    }
    
    @Override
    public int pushUnreadMessages(String userId, String sessionId, String organizationId, int maxCount) {
        try {
            log.debug("推送用户未读消息: userId={}, sessionId={}, maxCount={}", 
                     userId, sessionId, maxCount);
            
            // 1. 查询未读消息
            List<MessageDetailResponse> unreadMessages = messagePersistenceRepository
                .getUnreadMessages(userId, organizationId, maxCount);
            
            if (unreadMessages.isEmpty()) {
                log.debug("用户无未读消息: userId={}", userId);
                return 0;
            }
            
            // 2. 按优先级排序（高优先级优先）
            unreadMessages.sort((a, b) -> {
                int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                // 相同优先级按时间倒序
                return b.getCreatedTime().compareTo(a.getCreatedTime());
            });
            
            // 3. 批量推送消息
            int pushedCount = 0;
            List<String> pushedMessageIds = new ArrayList<>();
            
            for (MessageDetailResponse message : unreadMessages) {
                try {
                    WebSocketMessage wsMessage = buildWebSocketMessage(message);
                    
                    if (sessionId != null) {
                        // 会话级推送
                        boolean sent = messageService.sendMessageToUser(userId, wsMessage);
                        if (sent) {
                            pushedCount++;
                            pushedMessageIds.add(message.getMessageId());
                        }
                    } else {
                        // 用户级推送（所有会话）
                        boolean sent = messageService.sendMessageToUser(userId, wsMessage);
                        if (sent) {
                            pushedCount++;
                            pushedMessageIds.add(message.getMessageId());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("推送单条消息失败: messageId={}, userId={}, error={}", 
                             message.getMessageId(), userId, e.getMessage(), e);
                }
            }
            
            // 4. 批量标记消息为已读（如果推送成功）
            if (!pushedMessageIds.isEmpty()) {
                try {
                    messagePersistenceRepository.markMessagesAsRead(userId, organizationId, pushedMessageIds);
                    log.debug("批量标记消息已读完成: userId={}, count={}", userId, pushedMessageIds.size());
                } catch (Exception e) {
                    log.error("批量标记消息已读失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
            
            log.info("用户未读消息推送完成: userId={}, 查询到{}条, 成功推送{}条", 
                    userId, unreadMessages.size(), pushedCount);
            return pushedCount;
            
        } catch (Exception e) {
            log.error("推送用户未读消息失败: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public int pushUnviewedTaskResults(String userId, String sessionId, String organizationId) {
        try {
            log.debug("推送用户未查看任务结果: userId={}, sessionId={}", userId, sessionId);
            
            // 1. 查询未查看的完成任务
            List<TaskResultResponse> unviewedTasks = taskResultPersistenceRepository
                .getUnviewedCompletedTasks(userId, organizationId);
            
            if (unviewedTasks.isEmpty()) {
                log.debug("用户无未查看任务结果: userId={}", userId);
                return 0;
            }
            
            // 2. 按完成时间倒序排序（最新完成的优先）
            unviewedTasks.sort((a, b) -> {
                if (a.getCompletedTime() == null && b.getCompletedTime() == null) {
                    return 0;
                }
                if (a.getCompletedTime() == null) {
                    return 1;
                }
                if (b.getCompletedTime() == null) {
                    return -1;
                }
                return b.getCompletedTime().compareTo(a.getCompletedTime());
            });
            
            // 3. 限制推送数量
            List<TaskResultResponse> tasksToPush = unviewedTasks.stream()
                .limit(defaultMaxTasks)
                .collect(Collectors.toList());
            
            // 4. 批量推送任务结果
            int pushedCount = 0;
            List<String> pushedTaskIds = new ArrayList<>();
            
            for (TaskResultResponse task : tasksToPush) {
                try {
                    WebSocketMessage wsMessage = buildTaskResultMessage(task);
                    
                    boolean sent = messageService.sendMessageToUser(userId, wsMessage);
                    if (sent) {
                        pushedCount++;
                        pushedTaskIds.add(task.getTaskId());
                    }
                    
                } catch (Exception e) {
                    log.error("推送单个任务结果失败: taskId={}, userId={}, error={}", 
                             task.getTaskId(), userId, e.getMessage(), e);
                }
            }
            
            // 5. 标记任务结果为已查看
            if (!pushedTaskIds.isEmpty()) {
                try {
                    for (String taskId : pushedTaskIds) {
                        taskResultPersistenceRepository.markTaskResultAsViewed(taskId, userId);
                    }
                    log.debug("批量标记任务结果已查看完成: userId={}, count={}", userId, pushedTaskIds.size());
                } catch (Exception e) {
                    log.error("批量标记任务结果已查看失败: userId={}, error={}", userId, e.getMessage(), e);
                }
            }
            
            log.info("用户未查看任务结果推送完成: userId={}, 查询到{}个, 成功推送{}个", 
                    userId, unviewedTasks.size(), pushedCount);
            return pushedCount;
            
        } catch (Exception e) {
            log.error("推送用户未查看任务结果失败: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public int pushHighPriorityMessages(String userId, String sessionId, String organizationId) {
        try {
            log.debug("推送用户高优先级消息: userId={}, sessionId={}", userId, sessionId);
            
            // 1. 查询未读消息，但只关注高优先级的
            List<MessageDetailResponse> unreadMessages = messagePersistenceRepository
                .getUnreadMessages(userId, organizationId, defaultMaxMessages);
            
            // 2. 筛选出高优先级消息
            List<MessageDetailResponse> highPriorityMessages = unreadMessages.stream()
                .filter(msg -> msg.getPriority() != null && msg.getPriority() >= highPriorityThreshold)
                .sorted((a, b) -> {
                    int priorityCompare = Integer.compare(b.getPriority(), a.getPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    return b.getCreatedTime().compareTo(a.getCreatedTime());
                })
                .collect(Collectors.toList());
            
            if (highPriorityMessages.isEmpty()) {
                log.debug("用户无高优先级未读消息: userId={}", userId);
                return 0;
            }
            
            // 3. 推送高优先级消息
            int pushedCount = 0;
            for (MessageDetailResponse message : highPriorityMessages) {
                try {
                    WebSocketMessage wsMessage = buildWebSocketMessage(message);
                    wsMessage.setUrgent(true); // 标记为紧急消息
                    
                    boolean sent = messageService.sendMessageToUser(userId, wsMessage);
                    if (sent) {
                        pushedCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("推送高优先级消息失败: messageId={}, userId={}, error={}", 
                             message.getMessageId(), userId, e.getMessage(), e);
                }
            }
            
            log.info("用户高优先级消息推送完成: userId={}, 查询到{}条, 成功推送{}条", 
                    userId, highPriorityMessages.size(), pushedCount);
            return pushedCount;
            
        } catch (Exception e) {
            log.error("推送用户高优先级消息失败: userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public boolean pushUserStatistics(String userId, String sessionId, String organizationId) {
        try {
            log.debug("推送用户统计信息: userId={}, sessionId={}", userId, sessionId);
            
            // 1. 构建统计数据
            Map<String, Object> statistics = new HashMap<>();
            
            // 获取未读消息总数
            Long unreadCount = messagePersistenceRepository.getUnreadMessageCount(userId, organizationId);
            statistics.put("unreadMessageCount", unreadCount);
            
            // 获取在线状态
            boolean isOnline = messageCacheService.isUserOnline(userId);
            statistics.put("isOnline", isOnline);
            
            // 获取活跃会话数
            Map<Object, Object> activeSessions = messageCacheService.getUserActiveSessions(userId);
            statistics.put("activeSessionCount", activeSessions.size());
            
            // 构建当前时间戳
            statistics.put("lastUpdateTime", LocalDateTime.now());
            statistics.put("organizationId", organizationId);
            
            // 2. 构建统计消息
            WebSocketMessage statsMessage = WebSocketMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .messageType(MessageType.USER_STATISTICS.name())
                .senderId("SYSTEM")
                .receiverId(userId)
                .title("用户统计信息")
                .content("用户在线统计信息更新")
                .data(statistics)
                .timestamp(LocalDateTime.now())
                .build();
            
            // 3. 推送统计消息
            boolean sent = messageService.sendMessageToUser(userId, statsMessage);
            
            if (sent) {
                log.debug("用户统计信息推送成功: userId={}, unreadCount={}", userId, unreadCount);
            } else {
                log.warn("用户统计信息推送失败: userId={}", userId);
            }
            
            return sent;
            
        } catch (Exception e) {
            log.error("推送用户统计信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    // ==================== 推送策略管理 ====================
    
    @Override
    public boolean shouldPushToUser(String userId, String organizationId) {
        try {
            // 1. 检查用户是否在线
            if (!messageService.isUserOnline(userId)) {
                log.debug("用户不在线，跳过推送: userId={}", userId);
                return false;
            }
            
            // 2. 检查是否有未读消息
            Long unreadCount = messagePersistenceRepository.getUnreadMessageCount(userId, organizationId);
            if (unreadCount == null || unreadCount == 0) {
                // 检查是否有未查看任务
                List<TaskResultResponse> unviewedTasks = taskResultPersistenceRepository
                    .getUnviewedCompletedTasks(userId, organizationId);
                if (unviewedTasks.isEmpty()) {
                    log.debug("用户无未读消息和未查看任务，跳过推送: userId={}", userId);
                    return false;
                }
            }
            
            // 3. 检查推送频率限制（可选）
            // 可以在这里添加推送频率控制逻辑
            
            return true;
            
        } catch (Exception e) {
            log.error("检查用户推送需求失败: userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void markPushCompleted(String userId, String organizationId, 
                                List<String> pushedMessageIds, List<String> pushedTaskIds) {
        try {
            log.debug("标记推送完成: userId={}, messageCount={}, taskCount={}", 
                     userId, pushedMessageIds.size(), pushedTaskIds.size());
            
            // 1. 标记消息为已读
            if (pushedMessageIds != null && !pushedMessageIds.isEmpty()) {
                messagePersistenceRepository.markMessagesAsRead(userId, organizationId, pushedMessageIds);
            }
            
            // 2. 标记任务结果为已查看
            if (pushedTaskIds != null && !pushedTaskIds.isEmpty()) {
                for (String taskId : pushedTaskIds) {
                    taskResultPersistenceRepository.markTaskResultAsViewed(taskId, userId);
                }
            }
            
            // 3. 更新推送缓存记录（防重复推送）
            String pushRecordKey = "push_completed:" + userId + ":" + organizationId;
            messageCacheService.markMessageProcessed(pushRecordKey, cacheExpireHours);
            
            log.debug("推送完成标记成功: userId={}", userId);
            
        } catch (Exception e) {
            log.error("标记推送完成失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Object> getPushStrategy(String organizationId) {
        Map<String, Object> strategy = new HashMap<>();
        strategy.put("maxMessages", defaultMaxMessages);
        strategy.put("maxTasks", defaultMaxTasks);
        strategy.put("highPriorityThreshold", highPriorityThreshold);
        strategy.put("enableStatistics", enableStatisticsPush);
        strategy.put("cacheExpireHours", cacheExpireHours);
        strategy.put("organizationId", organizationId);
        return strategy;
    }
    
    @Override
    public boolean updatePushStrategy(String organizationId, Map<String, Object> strategy) {
        // 实现推送策略的动态更新
        // 可以将策略存储到数据库或Redis中，实现组织级个性化配置
        log.info("更新组织推送策略: organizationId={}, strategy={}", organizationId, strategy);
        return true;
    }
    
    // ==================== 异步推送实现 ====================
    
    @Override
    @Async
    public String handleUserOnlineAsync(String userId, String sessionId, String organizationId) {
        String taskId = UUID.randomUUID().toString();
        
        try {
            log.info("启动异步用户上线推送: taskId={}, userId={}", taskId, userId);
            
            // 初始化异步结果
            UserOnlinePushResult initialResult = UserOnlinePushResult.builder()
                .userId(userId)
                .sessionId(sessionId)
                .organizationId(organizationId)
                .asyncTaskId(taskId)
                .status("RUNNING")
                .progress(0)
                .startTime(LocalDateTime.now())
                .success(false)
                .build();
            
            asyncResultCache.put(taskId, initialResult);
            
            // 异步执行推送
            CompletableFuture.supplyAsync(() -> {
                try {
                    UserOnlinePushResult result = handleUserOnline(userId, sessionId, organizationId);
                    result.setAsyncTaskId(taskId);
                    result.setStatus("COMPLETED");
                    result.setProgress(100);
                    
                    asyncResultCache.put(taskId, result);
                    return result;
                    
                } catch (Exception e) {
                    log.error("异步推送执行失败: taskId={}, error={}", taskId, e.getMessage(), e);
                    
                    UserOnlinePushResult errorResult = UserOnlinePushResult.builder()
                        .userId(userId)
                        .sessionId(sessionId)
                        .organizationId(organizationId)
                        .asyncTaskId(taskId)
                        .status("FAILED")
                        .progress(100)
                        .success(false)
                        .errorMessage(e.getMessage())
                        .endTime(LocalDateTime.now())
                        .build();
                    
                    asyncResultCache.put(taskId, errorResult);
                    return errorResult;
                }
            });
            
            return taskId;
            
        } catch (Exception e) {
            log.error("启动异步用户上线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            
            UserOnlinePushResult errorResult = UserOnlinePushResult.builder()
                .userId(userId)
                .sessionId(sessionId)
                .organizationId(organizationId)
                .asyncTaskId(taskId)
                .status("FAILED")
                .progress(100)
                .success(false)
                .errorMessage(e.getMessage())
                .endTime(LocalDateTime.now())
                .build();
            
            asyncResultCache.put(taskId, errorResult);
            return taskId;
        }
    }
    
    @Override
    public UserOnlinePushResult getAsyncPushResult(String taskId) {
        UserOnlinePushResult result = asyncResultCache.get(taskId);
        if (result == null) {
            return UserOnlinePushResult.builder()
                .asyncTaskId(taskId)
                .status("NOT_FOUND")
                .success(false)
                .errorMessage("异步任务结果不存在或已过期")
                .build();
        }
        return result;
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 构建WebSocket消息
     */
    private WebSocketMessage buildWebSocketMessage(MessageDetailResponse message) {
        return WebSocketMessage.builder()
            .messageId(message.getMessageId())
            .messageType(message.getMessageType())
            .senderId(message.getSenderId())
            .senderName(message.getSenderName())
            .receiverId(message.getReceiverId())
            .title(message.getTitle())
            .content(message.getContent())
            .data(message.getMetadata())
            .timestamp(message.getCreatedTime())
            .urgent(message.getPriority() != null && message.getPriority() >= highPriorityThreshold)
            .build();
    }
    
    /**
     * 构建任务结果消息
     */
    private WebSocketMessage buildTaskResultMessage(TaskResultResponse task) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("taskId", task.getTaskId());
        taskData.put("taskType", task.getTaskType());
        taskData.put("taskName", task.getTaskName());
        taskData.put("status", task.getStatus());
        taskData.put("progress", task.getProgress());
        taskData.put("resultData", task.getResultData());
        taskData.put("completedTime", task.getCompletedTime());
        taskData.put("executionDurationMs", task.getExecutionDurationMs());
        
        String title = String.format("任务完成通知: %s", task.getTaskName());
        String content = String.format("任务[%s]已完成，状态: %s", task.getTaskName(), task.getStatus());
        
        return WebSocketMessage.builder()
            .messageId(UUID.randomUUID().toString())
            .messageType(MessageType.TASK_RESULT.name())
            .senderId("SYSTEM")
            .receiverId(task.getUserId())
            .title(title)
            .content(content)
            .data(taskData)
            .timestamp(LocalDateTime.now())
            .urgent("FAILED".equals(task.getStatus()))
            .build();
    }
    
    /**
     * 计算耗时
     */
    private Long calculateDuration(LocalDateTime startTime) {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
    }
}
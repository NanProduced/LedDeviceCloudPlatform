package org.nan.cloud.file.infrastructure.progress;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 转码进度WebSocket处理器
 * 
 * 功能说明：
 * - 管理WebSocket连接
 * - 推送转码进度信息
 * - 支持多用户并发监听
 * - 自动清理过期连接
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingProgressWebSocketHandler implements WebSocketHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // 存储所有活跃的WebSocket连接
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // 存储用户订阅的任务ID
    private final ConcurrentHashMap<String, String> userTaskMapping = new ConcurrentHashMap<>();
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userId = getUserIdFromSession(session);
        
        log.info("转码进度WebSocket连接建立 - 会话ID: {}, 用户ID: {}", sessionId, userId);
        
        sessions.put(sessionId, session);
        
        // 发送连接成功消息
        ProgressMessage connectMessage = ProgressMessage.builder()
                .type("CONNECT")
                .message("转码进度监听连接成功")
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendMessage(session, connectMessage);
        
        // 启动心跳检测
        startHeartbeat(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        log.debug("收到WebSocket消息 - 会话: {}, 内容: {}", session.getId(), payload);
        
        try {
            ProgressSubscribeRequest request = objectMapper.readValue(payload, ProgressSubscribeRequest.class);
            handleSubscribeRequest(session, request);
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            
            ProgressMessage errorMessage = ProgressMessage.builder()
                    .type("ERROR")
                    .message("消息格式错误: " + e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            sendMessage(session, errorMessage);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误 - 会话: {}", session.getId(), exception);
        closeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        log.info("转码进度WebSocket连接关闭 - 会话: {}, 状态: {}", sessionId, closeStatus);
        
        sessions.remove(sessionId);
        userTaskMapping.remove(sessionId);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 处理订阅请求
     */
    private void handleSubscribeRequest(WebSocketSession session, ProgressSubscribeRequest request) {
        String sessionId = session.getId();
        String taskId = request.getTaskId();
        String action = request.getAction();
        
        log.info("处理进度订阅请求 - 会话: {}, 任务: {}, 操作: {}", sessionId, taskId, action);
        
        if ("SUBSCRIBE".equals(action)) {
            // 订阅任务进度
            userTaskMapping.put(sessionId, taskId);
            
            // 立即发送当前进度
            sendCurrentProgress(session, taskId);
            
            // 启动进度推送
            startProgressPush(session, taskId);
            
        } else if ("UNSUBSCRIBE".equals(action)) {
            // 取消订阅
            userTaskMapping.remove(sessionId);
        }
        
        // 发送确认消息
        ProgressMessage confirmMessage = ProgressMessage.builder()
                .type("SUBSCRIBE_CONFIRM")
                .taskId(taskId)
                .message(action + " 成功")
                .timestamp(System.currentTimeMillis())
                .build();
        
        sendMessage(session, confirmMessage);
    }

    /**
     * 发送当前进度
     */
    private void sendCurrentProgress(WebSocketSession session, String taskId) {
        try {
            String progressKey = "transcoding:progress:" + taskId;
            Object progressData = redisTemplate.opsForValue().get(progressKey);
            
            if (progressData != null) {
                ProgressMessage progressMessage = ProgressMessage.builder()
                        .type("PROGRESS")
                        .taskId(taskId)
                        .data(progressData)
                        .timestamp(System.currentTimeMillis())
                        .build();
                
                sendMessage(session, progressMessage);
            }
        } catch (Exception e) {
            log.error("发送当前进度失败 - 任务: {}", taskId, e);
        }
    }

    /**
     * 启动进度推送
     */
    private void startProgressPush(WebSocketSession session, String taskId) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 检查会话是否还存在且订阅的是该任务
                if (!sessions.containsKey(session.getId()) || 
                    !taskId.equals(userTaskMapping.get(session.getId()))) {
                    return;
                }
                
                // 从Redis获取最新进度
                String progressKey = "transcoding:progress:" + taskId;
                Object progressData = redisTemplate.opsForValue().get(progressKey);
                
                if (progressData != null) {
                    ProgressMessage progressMessage = ProgressMessage.builder()
                            .type("PROGRESS")
                            .taskId(taskId)
                            .data(progressData)
                            .timestamp(System.currentTimeMillis())
                            .build();
                    
                    sendMessage(session, progressMessage);
                    
                    // 检查是否已完成
                    if (isTaskCompleted(progressData)) {
                        // 任务完成，停止推送
                        userTaskMapping.remove(session.getId());
                    }
                }
                
            } catch (Exception e) {
                log.error("推送进度失败 - 任务: {}", taskId, e);
            }
        }, 1, 2, TimeUnit.SECONDS); // 每2秒推送一次
    }

    /**
     * 启动心跳检测
     */
    private void startHeartbeat(WebSocketSession session) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (sessions.containsKey(session.getId()) && session.isOpen()) {
                    ProgressMessage heartbeat = ProgressMessage.builder()
                            .type("HEARTBEAT")
                            .message("ping")
                            .timestamp(System.currentTimeMillis())
                            .build();
                    
                    sendMessage(session, heartbeat);
                } else {
                    closeSession(session);
                }
            } catch (Exception e) {
                log.error("心跳检测失败", e);
                closeSession(session);
            }
        }, 30, 30, TimeUnit.SECONDS); // 每30秒发送心跳
    }

    /**
     * 发送消息到WebSocket客户端
     */
    private void sendMessage(WebSocketSession session, ProgressMessage message) {
        try {
            if (session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("发送WebSocket消息 - 会话: {}, 类型: {}", session.getId(), message.getType());
            }
        } catch (IOException e) {
            log.error("发送WebSocket消息失败 - 会话: {}", session.getId(), e);
            closeSession(session);
        }
    }

    /**
     * 关闭会话
     */
    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.error("关闭WebSocket会话失败", e);
        } finally {
            sessions.remove(session.getId());
            userTaskMapping.remove(session.getId());
        }
    }

    /**
     * 从会话中获取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
        // 从查询参数或头部获取用户ID
        // 这里需要根据实际的认证方式来实现
        return session.getUri().getQuery(); // 简化实现
    }

    /**
     * 检查任务是否已完成
     */
    private boolean isTaskCompleted(Object progressData) {
        try {
            String jsonStr = objectMapper.writeValueAsString(progressData);
            // 解析进度数据，检查状态
            return jsonStr.contains("\"status\":\"COMPLETED\"") || 
                   jsonStr.contains("\"status\":\"FAILED\"") ||
                   jsonStr.contains("\"progress\":100");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 广播进度更新（供外部调用）
     */
    public void broadcastProgress(String taskId, Object progressData) {
        ProgressMessage progressMessage = ProgressMessage.builder()
                .type("PROGRESS")
                .taskId(taskId)
                .data(progressData)
                .timestamp(System.currentTimeMillis())
                .build();

        sessions.values().parallelStream()
                .filter(session -> taskId.equals(userTaskMapping.get(session.getId())))
                .forEach(session -> sendMessage(session, progressMessage));
    }

    /**
     * 进度消息
     */
    public static class ProgressMessage {
        private String type;
        private String taskId;
        private String message;
        private Object data;
        private long timestamp;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ProgressMessage message = new ProgressMessage();

            public Builder type(String type) {
                message.type = type;
                return this;
            }

            public Builder taskId(String taskId) {
                message.taskId = taskId;
                return this;
            }

            public Builder message(String msg) {
                message.message = msg;
                return this;
            }

            public Builder data(Object data) {
                message.data = data;
                return this;
            }

            public Builder timestamp(long timestamp) {
                message.timestamp = timestamp;
                return this;
            }

            public ProgressMessage build() {
                return message;
            }
        }

        // Getters
        public String getType() { return type; }
        public String getTaskId() { return taskId; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 进度订阅请求
     */
    public static class ProgressSubscribeRequest {
        private String action; // SUBSCRIBE/UNSUBSCRIBE
        private String taskId;

        // Getters and Setters
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
    }
}
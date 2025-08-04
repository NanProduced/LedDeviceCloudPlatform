package org.nan.cloud.message.infrastructure.websocket.processor.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.message.api.stomp.CommonStompMessage;
import org.nan.cloud.message.api.stomp.StompMessageLevel;
import org.nan.cloud.message.api.stomp.StompMessageTypes;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.DispatchResult;
import org.nan.cloud.message.infrastructure.websocket.dispatcher.StompMessageDispatcher;
import org.nan.cloud.message.infrastructure.websocket.processor.BusinessMessageProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件上传消息处理器
 * 
 * 负责处理来自file-service和core-service的文件上传相关消息，
 * 将其转换为STOMP消息并推送给前端用户。
 * 
 * 支持的消息类型：
 * 1. 文件上传开始消息 (file.upload.started)
 * 2. 文件上传进度消息 (file.upload.progress)  
 * 3. 文件上传完成消息 (file.upload.completed)
 * 4. 文件上传失败消息 (file.upload.failed)
 * 5. 文件处理开始消息 (file.upload.processing)
 * 
 * 消息路由：
 * - 路由键格式：file.upload.{eventType}.{orgId}.{userId}
 * - STOMP目标：/user/{userId}/queue/file-upload
 * 
 * @author LedDeviceCloudPlatform Team  
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadMessageProcessor implements BusinessMessageProcessor {

    private final StompMessageDispatcher stompMessageDispatcher;
    
    /**
     * 批量上传进度聚合缓存
     * Key: batchId_userId, Value: BatchProgressTracker
     */
    private final Map<String, BatchProgressTracker> batchProgressMap = new ConcurrentHashMap<>();
    
    /**
     * 进度消息去重缓存 - 防止频繁推送相同进度
     * Key: taskId_progress, Value: lastSentTime
     */
    private final Map<String, Long> progressDeduplicationMap = new ConcurrentHashMap<>();
    
    /**
     * 进度推送间隔阈值（毫秒）- 相同进度1秒内只推送一次
     */
    private static final long PROGRESS_THROTTLE_MILLIS = 1000;

    @Override
    public String getSupportedMessageType() {
        return "FILE_UPLOAD";
    }

    @Override
    public boolean supports(String messageType, String routingKey) {
        // 支持所有以 "FILE_UPLOAD" 开头的消息类型
        return messageType != null && messageType.startsWith("FILE_UPLOAD");
    }

    @Override
    public int getPriority() {
        return 50; // 中等优先级
    }

    @Override
    public BusinessMessageProcessResult process(String messagePayload, String routingKey) {
        try {
            log.debug("开始处理文件上传消息 - 路由键: {}", routingKey);
            
            // 解析路由键: stomp.file.upload.{orgId}.{userId}
            String[] routeParts = routingKey.split("\\.");
            if (routeParts.length < 5) {
                String errorMsg = "文件上传消息路由键格式错误: " + routingKey;
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            String orgId = routeParts[3];
            String userId = routeParts[4];
            
            // 解析消息载荷
            Map<String, Object> messageData;
            try {
                messageData = JsonUtils.fromJson(messagePayload, Map.class);
                if (messageData == null) {
                    String errorMsg = "文件上传消息载荷解析结果为null";
                    log.warn("⚠️ {} - 原始载荷: {}", errorMsg, messagePayload);
                    return BusinessMessageProcessResult.failure(null, errorMsg);
                }
            } catch (Exception e) {
                String errorMsg = "文件上传消息载荷JSON解析异常: " + e.getMessage();
                log.warn("⚠️ {} - 原始载荷: {}", errorMsg, messagePayload, e);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 从载荷中获取事件类型
            String eventType = (String) messageData.get("eventType");
            if (eventType == null || eventType.trim().isEmpty()) {
                String errorMsg = "文件上传消息中缺少eventType字段或为空";
                log.warn("⚠️ {} - 消息数据: {}", errorMsg, messageData);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 标准化事件类型格式 (转换为大写)
            eventType = eventType.toUpperCase().trim();
            
            // 处理批量上传进度聚合
            if ("PROGRESS".equals(eventType)) {
                BatchProgressResult batchResult = handleBatchProgressAggregation(messageData, orgId, userId);
                if (batchResult.shouldSkip()) {
                    log.debug("跳过重复进度消息 - 批次: {}, 用户: {}", batchResult.getBatchId(), userId);
                    return BusinessMessageProcessResult.success("skipped", null, null);
                }
                messageData = batchResult.getAggregatedData();
            }
            
            // 根据事件类型创建不同的STOMP消息
            CommonStompMessage stompMessage = createStompMessage(eventType, messageData, orgId, userId);
            if (stompMessage == null) {
                String errorMsg = "不支持的文件上传事件类型: " + eventType;
                log.warn("⚠️ {}", errorMsg);
                return BusinessMessageProcessResult.failure(null, errorMsg);
            }
            
            // 分发消息到用户队列
            try {
                stompMessageDispatcher.sendToUser(userId, stompMessage);
                log.info("✅ 文件上传消息处理成功 - 事件: {}, 用户: {}, 消息ID: {}", 
                        eventType, userId, stompMessage.getMessageId());
                return BusinessMessageProcessResult.success(stompMessage.getMessageId(), null, stompMessage);
            } catch (Exception e) {
                String errorMsg = "STOMP消息分发失败: " + e.getMessage();
                log.error("❌ {} - 事件: {}, 用户: {}, 消息ID: {}", 
                        errorMsg, eventType, userId, stompMessage.getMessageId(), e);
                return BusinessMessageProcessResult.failure(stompMessage.getMessageId(), errorMsg);
            }
            
        } catch (Exception e) {
            String errorMsg = "文件上传消息处理异常: " + e.getMessage();
            log.error("💥 {} - 路由键: {}", errorMsg, routingKey, e);
            return BusinessMessageProcessResult.failure(null, errorMsg);
        }
    }

    /**
     * 根据事件类型创建STOMP消息
     */
    private CommonStompMessage createStompMessage(String eventType, Map<String, Object> messageData, 
                                                String orgId, String userId) {
        String messageId = "file_upload_" + System.currentTimeMillis();
        LocalDateTime timestamp = LocalDateTime.now();
        // message-service只接收上传进度、上传结果消息
        switch (eventType) {
            case "PROGRESS":
                // 检查是否是批量上传进度
                if (messageData.containsKey("batchId") && messageData.get("batchId") != null) {
                    return createBatchUploadProgressMessage(messageId, messageData, orgId, userId, timestamp);
                } else {
                    return createUploadProgressMessage(messageId, messageData, orgId, userId, timestamp);
                }
            case "COMPLETED":
                return createUploadCompletedMessage(messageId, messageData, orgId, userId, timestamp);
            case "FAILED":
                return createUploadFailedMessage(messageId, messageData, orgId, userId, timestamp);
            default:
                return null;
        }
    }


    /**
     * 创建上传进度消息
     */
    private CommonStompMessage createUploadProgressMessage(String messageId, Map<String, Object> messageData,
                                                         String orgId, String userId, LocalDateTime timestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", messageData.get("eventType"));
        payload.put("taskId", messageData.get("taskId"));
        payload.put("fileName", messageData.get("originalFilename"));
        payload.put("progress", messageData.get("progress"));
        
        // 确保uploadedBytes和totalBytes字段存在
        Object uploadedBytesObj = messageData.get("uploadedBytes");
        Object totalBytesObj = messageData.get("totalBytes");
        payload.put("uploadedBytes", uploadedBytesObj != null ? uploadedBytesObj : 0L);
        payload.put("totalBytes", totalBytesObj != null ? totalBytesObj : 0L);
        payload.put("timestamp", timestamp);
        
        Object progress = messageData.get("progress");
        String progressText = progress != null ? progress + "%" : "未知";
        String filename = (String) messageData.get("originalFilename");
        filename = filename != null ? filename : "未知文件";
        
        return CommonStompMessage.builder()
                .messageId(messageId)
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .level(StompMessageLevel.INFO)
                .title("文件上传进度")
                .content("文件 " + filename + " 上传进度: " + progressText)
                .payload(payload)
                .timestamp(timestamp.toString())
                .oid(Long.valueOf(orgId))
                .context(CommonStompMessage.Context.fileContext(
                        Long.valueOf(userId), 
                        (String) messageData.get("taskId")))
                .build();
    }

    /**
     * 创建上传完成消息
     */
    private CommonStompMessage createUploadCompletedMessage(String messageId, Map<String, Object> messageData,
                                                          String orgId, String userId, LocalDateTime timestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", messageData.get("eventType"));
        payload.put("taskId", messageData.get("taskId"));
        payload.put("fileId", messageData.get("fileId"));
        payload.put("fileName", messageData.get("originalFilename"));
        payload.put("materialId", messageData.get("materialId"));
        payload.put("timestamp", timestamp);
        
        return CommonStompMessage.builder()
                .messageId(messageId)
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .level(StompMessageLevel.SUCCESS)
                .title("文件上传完成")
                .content("文件 " + messageData.get("originalFilename") + " 上传并处理完成")
                .payload(payload)
                .timestamp(timestamp.toString())
                .oid(Long.valueOf(orgId))
                .context(CommonStompMessage.Context.fileContext(
                        Long.valueOf(userId), 
                        (String) messageData.get("taskId")))
                .build();
    }

    /**
     * 创建上传失败消息
     */
    private CommonStompMessage createUploadFailedMessage(String messageId, Map<String, Object> messageData,
                                                       String orgId, String userId, LocalDateTime timestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", messageData.get("eventType"));
        payload.put("taskId", messageData.get("taskId"));
        payload.put("fileName", messageData.get("originalFilename"));
        payload.put("errorCode", messageData.get("errorCode"));
        payload.put("errorMessage", messageData.get("errorMessage"));
        payload.put("timestamp", timestamp);
        
        return CommonStompMessage.builder()
                .messageId(messageId)
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .level(StompMessageLevel.ERROR)
                .title("文件上传失败")
                .content("文件 " + messageData.get("originalFilename") + " 上传失败: " + messageData.get("errorMessage"))
                .payload(payload)
                .timestamp(timestamp.toString())
                .oid(Long.valueOf(orgId))
                .context(CommonStompMessage.Context.fileContext(
                        Long.valueOf(userId), 
                        (String) messageData.get("taskId")))
                .build();
    }

    
    // ==================== 批量进度处理逻辑 ====================
    
    /**
     * 处理批量上传进度聚合
     * 
     * @param messageData 原始消息数据
     * @param orgId 组织ID
     * @param userId 用户ID
     * @return 批量进度处理结果
     */
    private BatchProgressResult handleBatchProgressAggregation(Map<String, Object> messageData, String orgId, String userId) {
        String taskId = (String) messageData.get("taskId");
        String batchId = (String) messageData.get("batchId");
        
        // 如果没有batchId，则是单文件上传，直接处理
        if (batchId == null || batchId.isEmpty()) {
            return handleSingleFileProgress(messageData, taskId);
        }
        
        // 批量上传进度聚合处理
        String batchKey = batchId + "_" + userId;
        BatchProgressTracker tracker = batchProgressMap.computeIfAbsent(batchKey, 
                k -> new BatchProgressTracker(batchId, userId, orgId));
        
        // 更新单个文件的进度
        Object progressObj = messageData.get("progress");
        Object uploadedBytesObj = messageData.get("uploadedBytes");
        Object totalBytesObj = messageData.get("totalBytes");
        
        if (progressObj != null && uploadedBytesObj != null && totalBytesObj != null) {
            double progress = parseDouble(progressObj);
            long uploadedBytes = parseLong(uploadedBytesObj);
            long totalBytes = parseLong(totalBytesObj);
            
            tracker.updateFileProgress(taskId, progress, uploadedBytes, totalBytes, 
                    (String) messageData.get("originalFilename"));
        }
        
        // 计算整体批次进度
        BatchProgressTracker.BatchProgressSummary summary = tracker.calculateBatchProgress();
        
        // 检查是否需要推送（避免频繁推送相同进度）
        String progressKey = batchId + "_" + (int) summary.getOverallProgress();
        long currentTime = System.currentTimeMillis();
        Long lastSentTime = progressDeduplicationMap.get(progressKey);
        
        if (lastSentTime != null && (currentTime - lastSentTime) < PROGRESS_THROTTLE_MILLIS) {
            // 相同进度在阈值时间内已推送过，跳过
            return BatchProgressResult.skip(batchId);
        }
        
        // 更新去重缓存
        progressDeduplicationMap.put(progressKey, currentTime);
        
        // 构建聚合后的消息数据
        Map<String, Object> aggregatedData = new HashMap<>(messageData);
        aggregatedData.put("batchId", batchId);
        aggregatedData.put("totalFiles", summary.getTotalFiles());
        aggregatedData.put("completedFiles", summary.getCompletedFiles());
        aggregatedData.put("failedFiles", summary.getFailedFiles());
        aggregatedData.put("overallProgress", summary.getOverallProgress());
        aggregatedData.put("totalUploadedBytes", summary.getTotalUploadedBytes());
        aggregatedData.put("totalBytes", summary.getTotalBytes());
        aggregatedData.put("currentFileName", messageData.get("originalFilename"));
        aggregatedData.put("batchProgressDetails", summary.getFileProgressDetails());
        
        log.debug("批量进度聚合 - 批次: {}, 总进度: {}%, 完成文件: {}/{}", 
                batchId, summary.getOverallProgress(), summary.getCompletedFiles(), summary.getTotalFiles());
        
        return BatchProgressResult.success(batchId, aggregatedData);
    }
    
    /**
     * 处理单文件进度（包含去重逻辑）
     */
    private BatchProgressResult handleSingleFileProgress(Map<String, Object> messageData, String taskId) {
        Object progressObj = messageData.get("progress");
        if (progressObj == null) {
            return BatchProgressResult.success(null, messageData);
        }
        
        int progress = (int) parseDouble(progressObj);
        String progressKey = taskId + "_" + progress;
        long currentTime = System.currentTimeMillis();
        Long lastSentTime = progressDeduplicationMap.get(progressKey);
        
        if (lastSentTime != null && (currentTime - lastSentTime) < PROGRESS_THROTTLE_MILLIS) {
            // 相同进度在阈值时间内已推送过，跳过
            return BatchProgressResult.skip(null);
        }
        
        // 更新去重缓存
        progressDeduplicationMap.put(progressKey, currentTime);
        
        return BatchProgressResult.success(null, messageData);
    }
    
    /**
     * 创建批量上传进度消息
     */
    private CommonStompMessage createBatchUploadProgressMessage(String messageId, Map<String, Object> messageData,
                                                               String orgId, String userId, LocalDateTime timestamp) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", messageData.get("eventType"));
        payload.put("batchId", messageData.get("batchId"));
        payload.put("taskId", messageData.get("taskId"));
        payload.put("currentFileName", messageData.get("currentFileName"));
        payload.put("totalFiles", messageData.get("totalFiles"));
        payload.put("completedFiles", messageData.get("completedFiles"));
        payload.put("failedFiles", messageData.get("failedFiles"));
        payload.put("overallProgress", messageData.get("overallProgress"));
        payload.put("totalUploadedBytes", messageData.get("totalUploadedBytes"));
        payload.put("totalBytes", messageData.get("totalBytes"));
        payload.put("batchProgressDetails", messageData.get("batchProgressDetails"));
        payload.put("timestamp", timestamp);
        
        Object overallProgress = messageData.get("overallProgress");
        Object totalFiles = messageData.get("totalFiles");
        Object completedFiles = messageData.get("completedFiles");
        
        String progressText = String.format("%.1f%% (%s/%s)", 
                parseDouble(overallProgress), completedFiles, totalFiles);
        
        return CommonStompMessage.builder()
                .messageId(messageId)
                .messageType(StompMessageTypes.TASK_PROGRESS)
                .level(StompMessageLevel.INFO)
                .title("批量文件上传进度")
                .content("批量上传进度: " + progressText + 
                        " - 当前文件: " + messageData.get("currentFileName"))
                .payload(payload)
                .timestamp(timestamp.toString())
                .oid(Long.valueOf(orgId))
                .context(CommonStompMessage.Context.fileContext(
                        Long.valueOf(userId), 
                        (String) messageData.get("batchId")))
                .build();
    }
    
    // ==================== 辅助方法 ====================
    
    private double parseDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    private long parseLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
    
    /**
     * 清理过期的批量进度跟踪器
     * 定期调用以避免内存泄露
     */
    public void cleanupExpiredBatchTrackers() {
        long expireTime = System.currentTimeMillis() - (2 * 60 * 60 * 1000); // 2小时过期
        
        batchProgressMap.entrySet().removeIf(entry -> {
            BatchProgressTracker tracker = entry.getValue();
            return tracker.getLastUpdateTime() < expireTime;
        });
        
        // 清理进度去重缓存
        progressDeduplicationMap.entrySet().removeIf(entry -> {
            return entry.getValue() < expireTime;
        });
        
        log.debug("清理过期批量进度跟踪器完成 - 剩余跟踪器: {}", batchProgressMap.size());
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 批量进度处理结果
     */
    private static class BatchProgressResult {
        private final String batchId;
        private final Map<String, Object> aggregatedData;
        private final boolean skip;
        
        private BatchProgressResult(String batchId, Map<String, Object> aggregatedData, boolean skip) {
            this.batchId = batchId;
            this.aggregatedData = aggregatedData;
            this.skip = skip;
        }
        
        public static BatchProgressResult success(String batchId, Map<String, Object> data) {
            return new BatchProgressResult(batchId, data, false);
        }
        
        public static BatchProgressResult skip(String batchId) {
            return new BatchProgressResult(batchId, null, true);
        }
        
        public String getBatchId() { return batchId; }
        public Map<String, Object> getAggregatedData() { return aggregatedData; }
        public boolean shouldSkip() { return skip; }
    }
    
    /**
     * 批量上传进度跟踪器
     */
    private static class BatchProgressTracker {
        private final String batchId;
        private final String userId;
        private final String orgId;
        private final Map<String, FileProgressInfo> fileProgressMap;
        private long lastUpdateTime;
        
        public BatchProgressTracker(String batchId, String userId, String orgId) {
            this.batchId = batchId;
            this.userId = userId;
            this.orgId = orgId;
            this.fileProgressMap = new ConcurrentHashMap<>();
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public void updateFileProgress(String taskId, double progress, long uploadedBytes, 
                                     long totalBytes, String filename) {
            FileProgressInfo fileInfo = fileProgressMap.computeIfAbsent(taskId, 
                    k -> new FileProgressInfo(taskId, filename));
            
            fileInfo.updateProgress(progress, uploadedBytes, totalBytes);
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public BatchProgressSummary calculateBatchProgress() {
            if (fileProgressMap.isEmpty()) {
                return new BatchProgressSummary(0, 0, 0, 0.0, 0L, 0L, new ArrayList<>());
            }
            
            int totalFiles = fileProgressMap.size();
            AtomicInteger completedFiles = new AtomicInteger();
            AtomicInteger failedFiles = new AtomicInteger();
            AtomicLong totalUploadedBytes = new AtomicLong(0);
            AtomicLong totalBytes = new AtomicLong(0);
            List<Map<String, Object>> progressDetails = new ArrayList<>();
            
            fileProgressMap.values().forEach(fileInfo -> {
                totalUploadedBytes.addAndGet(fileInfo.getUploadedBytes());
                totalBytes.addAndGet(fileInfo.getTotalBytes());
                
                // 创建文件进度详情
                Map<String, Object> detail = new HashMap<>();
                detail.put("taskId", fileInfo.getTaskId());
                detail.put("filename", fileInfo.getFilename());
                detail.put("progress", fileInfo.getProgress());
                detail.put("uploadedBytes", fileInfo.getUploadedBytes());
                detail.put("totalBytes", fileInfo.getTotalBytes());
                detail.put("status", fileInfo.getStatus());
                progressDetails.add(detail);
                
                if (fileInfo.getProgress() >= 100.0) {
                    completedFiles.getAndIncrement();
                } else if ("FAILED".equals(fileInfo.getStatus())) {
                    failedFiles.getAndIncrement();
                }
            });
            
            // 计算整体进度
            double overallProgress = totalBytes.get() > 0 ? 
                    (double) totalUploadedBytes.get() * 100.0 / totalBytes.get() : 0.0;
            
            return new BatchProgressSummary(totalFiles, completedFiles.get(), failedFiles.get(),
                    overallProgress, totalUploadedBytes.get(), totalBytes.get(), progressDetails);
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        /**
         * 文件进度信息
         */
        private static class FileProgressInfo {
            private final String taskId;
            private final String filename;
            private double progress;
            private long uploadedBytes;
            private long totalBytes;
            private String status;
            
            public FileProgressInfo(String taskId, String filename) {
                this.taskId = taskId;
                this.filename = filename;
                this.progress = 0.0;
                this.uploadedBytes = 0L;
                this.totalBytes = 0L;
                this.status = "IN_PROGRESS";
            }
            
            public void updateProgress(double progress, long uploadedBytes, long totalBytes) {
                this.progress = progress;
                this.uploadedBytes = uploadedBytes;
                this.totalBytes = totalBytes;
                
                if (progress >= 100.0) {
                    this.status = "COMPLETED";
                } else if (progress < 0) {
                    this.status = "FAILED";
                } else {
                    this.status = "IN_PROGRESS";
                }
            }
            
            // Getters
            public String getTaskId() { return taskId; }
            public String getFilename() { return filename; }
            public double getProgress() { return progress; }
            public long getUploadedBytes() { return uploadedBytes; }
            public long getTotalBytes() { return totalBytes; }
            public String getStatus() { return status; }
        }
        
        /**
         * 批量进度汇总信息
         */
        public static class BatchProgressSummary {
            private final int totalFiles;
            private final int completedFiles;
            private final int failedFiles;
            private final double overallProgress;
            private final long totalUploadedBytes;
            private final long totalBytes;
            private final List<Map<String, Object>> fileProgressDetails;
            
            public BatchProgressSummary(int totalFiles, int completedFiles, int failedFiles,
                                      double overallProgress, long totalUploadedBytes, long totalBytes,
                                      List<Map<String, Object>> fileProgressDetails) {
                this.totalFiles = totalFiles;
                this.completedFiles = completedFiles;
                this.failedFiles = failedFiles;
                this.overallProgress = overallProgress;
                this.totalUploadedBytes = totalUploadedBytes;
                this.totalBytes = totalBytes;
                this.fileProgressDetails = fileProgressDetails;
            }
            
            // Getters
            public int getTotalFiles() { return totalFiles; }
            public int getCompletedFiles() { return completedFiles; }
            public int getFailedFiles() { return failedFiles; }
            public double getOverallProgress() { return overallProgress; }
            public long getTotalUploadedBytes() { return totalUploadedBytes; }
            public long getTotalBytes() { return totalBytes; }
            public List<Map<String, Object>> getFileProgressDetails() { return fileProgressDetails; }
        }
    }
}
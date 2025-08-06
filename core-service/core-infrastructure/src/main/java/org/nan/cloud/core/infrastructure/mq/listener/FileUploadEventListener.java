package org.nan.cloud.core.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.core.event.mq.FileUploadEvent;
import org.nan.cloud.core.infrastructure.task.TaskStatusHandler;
import org.nan.cloud.core.service.MaterialService;
import org.nan.cloud.core.service.MaterialFileService;
import org.nan.cloud.core.domain.Material;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文件上传事件监听器
 * 
 * 职责：
 * 1. 监听file-service发布的文件上传事件
 * 2. 根据事件类型触发不同的业务处理
 * 3. 协调MaterialFile和Material的创建
 * 4. 触发上传进度和状态的消息推送
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadEventListener implements MessageConsumer {

    private final MaterialService materialService;
    private final MaterialFileService materialFileService;
    private final TaskStatusHandler taskStatusHandler;

    // * ============ ⚠️注意 ============= *//
    // Mq消息消费者只关注对应队列消费消息，不要在消费完成后再生产Mq消息给其他服务消费
    // 这里file-service生产的文件上传消息，如果需要推送至message-service，则直接推到message-service的队列
    // 不要经由core-service

    @Override
    public ConsumeResult consume(Message message) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("📥 收到文件上传事件 - 类型: {}, 消息ID: {}", 
                    message.getMessageType(), message.getMessageId());

            switch (message.getMessageType()) {
                case "FILE_UPLOAD_TASK_CREATED" -> handleUploadTaskCreated(message);
                case "FILE_UPLOAD_STARTED" -> handleUploadStarted(message);
                case "FILE_UPLOAD_PROGRESS" -> handleUploadProgress(message);
                case "FILE_UPLOAD_COMPLETED" -> handleUploadCompleted(message);
                case "FILE_UPLOAD_FAILED" -> handleUploadFailed(message);
                case "FILE_PROCESSING_STARTED" -> handleProcessingStarted(message);
                case "FILE_PROCESSING_COMPLETED" -> handleProcessingCompleted(message);
                default -> {
                    log.warn("⚠️ 未知的文件上传事件类型: {}", message.getMessageType());
                    return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                            "UNSUPPORTED_MESSAGE_TYPE", "不支持的消息类型: " + message.getMessageType(), null);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return ConsumeResult.success(message.getMessageId(), getConsumerId(), duration);
            
        } catch (Exception e) {
            log.error("❌ 处理文件上传事件失败 - 消息类型: {}, 错误: {}", 
                    message.getMessageType(), e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), 
                    "CONSUME_EXCEPTION", "处理文件上传事件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对应file-service推送Message的messageType
     * 统一将message中的payload转为FileUploadEvent
     * 通过FileUploadEvent中的eventType判读是哪种类型文件上传
     * @return
     */
    @Override
    public String[] getSupportedMessageTypes() {
        return new String[]{
            "FILE_UPLOAD_TASK_CREATED",
            "FILE_UPLOAD_STARTED",
            "FILE_UPLOAD_PROGRESS", 
            "FILE_UPLOAD_COMPLETED",
            "FILE_UPLOAD_FAILED",
            "FILE_PROCESSING_STARTED",
            "FILE_PROCESSING_COMPLETED",
            "THUMBNAIL_GENERATED"
        };
    }

    @Override
    public String getConsumerId() {
        return "FileUploadEventListener";
    }

    /**
     * 处理异步文件上传任务创建事件
     * 阶段1：创建Task和Material实体
     */
    private void handleUploadTaskCreated(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.info("🎯 异步文件上传任务创建 - 任务ID: {}, 文件ID: {}, 文件名: {}", 
                event.getTaskId(), event.getFileId(), event.getOriginalFilename());
        
        try {
            // 1. 初始化任务状态
            taskStatusHandler.initMaterialUploadTask(event);
            
            // 2. 创建Material实体 (与文件解耦，先创建占位)
            Long materialId = materialService.createPendingMaterialFromEvent(event);
            
            // 任务创建不推送到message-service
            // 且任务相关的进度、完成情况由file-service直接推送给message-service，不经过core-service
            
            log.info("✅ 异步上传任务初始化完成 - 任务ID: {}, 素材ID: {}", event.getTaskId(), materialId);
            
        } catch (Exception e) {
            log.error("❌ 异步上传任务创建失败 - 任务ID: {}, 错误: {}", 
                    event.getTaskId(), e.getMessage(), e);
            
            // 任务失败
            taskStatusHandler.failTask(event.getTaskId(), "任务创建失败: " + e.getMessage());
            
        }
    }

    /**
     * 处理文件上传开始事件
     */
    private void handleUploadStarted(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.info("🚀 文件上传开始 - 任务ID: {}, 文件名: {}", 
                event.getTaskId(), event.getOriginalFilename());
        
        // 文件上传开始不需要重复创建任务，任务已在 handleUploadTaskCreated 中创建
        // 只更新任务状态为进行中
        try {
            taskStatusHandler.updateTaskStatus(event.getTaskId(), TaskStatusEnum.RUNNING);
            log.info("✅ 任务状态更新为进行中 - 任务ID: {}", event.getTaskId());
        } catch (Exception e) {
            log.error("❌ 更新任务状态失败 - 任务ID: {}, 错误: {}", event.getTaskId(), e.getMessage(), e);
        }
    }

    /**
     * 处理文件上传进度事件
     */
    private void handleUploadProgress(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.debug("📊 文件上传进度 - 任务ID: {}, 进度: {}%", 
                event.getTaskId(), event.getProgress());
        
        // 更新任务进度
        taskStatusHandler.updateTaskProgress(event.getTaskId(), event.getProgress());

    }

    /**
     * 处理文件上传完成事件
     */
    private void handleUploadCompleted(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.info("✅ 文件上传完成 - 任务ID: {}, 文件ID: {}", 
                event.getTaskId(), event.getFileId());
        
        try {
            // 1. 创建或更新MaterialFile（文件实体信息）
            boolean fileCreated = materialFileService.createMaterialFile(event);
            if (!fileCreated) {
                log.error("创建MaterialFile失败 - 文件ID: {}", event.getFileId());
                taskStatusHandler.failTask(event.getTaskId(), "创建文件记录失败");
                return;
            }

            // 2. 检查是否已存在Material（避免重复创建）
            Material existingMaterial = materialService.getMaterialByFileId(event.getFileId());
            Long materialId;
            
            if (existingMaterial != null) {
                // 更新现有Material的业务信息
                materialService.updateMaterialFromFileUpload(existingMaterial.getMid(), event);
                materialId = existingMaterial.getMid();
                log.info("✅ 素材业务信息更新完成 - 素材ID: {}, 文件ID: {}", materialId, event.getFileId());
            }
            
            // 3. 完成任务
            taskStatusHandler.completeTask(event.getTaskId(), event.getThumbnailUrl());
            
        } catch (Exception e) {
            log.error("❌ 创建素材业务数据失败 - 任务ID: {}, 错误: {}", 
                    event.getTaskId(), e.getMessage(), e);
            
            // 任务失败
            taskStatusHandler.failTask(event.getTaskId(), "创建素材业务数据失败: " + e.getMessage());

        }
    }

    /**
     * 处理文件上传失败事件
     */
    private void handleUploadFailed(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.error("❌ 文件上传失败 - 任务ID: {}, 错误: {}", 
                event.getTaskId(), event.getErrorMessage());
        
        // 任务失败
        taskStatusHandler.failTask(event.getTaskId(), event.getErrorMessage());

    }

    /**
     * 处理文件处理开始事件
     */
    private void handleProcessingStarted(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.info("🔄 文件处理开始 - 任务ID: {}, 文件ID: {}", 
                event.getTaskId(), event.getFileId());

    }

    /**
     * 处理文件处理完成事件
     */
    private void handleProcessingCompleted(Message message) {
        FileUploadEvent event = parseEvent(message);
        log.info("✅ 文件处理完成 - 任务ID: {}, 文件ID: {}", 
                event.getTaskId(), event.getFileId());

        // 元数据解析完成事件
        if (event.getProcessType().equals("METADATA")) {

            try {
                // 更新元数据ID
                String metadataId = event.getMetadataId();
                if (metadataId != null) {
                    materialService.updateMaterialMetadata(event.getFileId(), metadataId);
                }

            } catch (Exception e) {
                log.error("❌ 更新素材元数据失败 - 任务ID: {}, 错误: {}",
                        event.getTaskId(), e.getMessage(), e);
            }
        }
    }

    /**
     * 解析事件数据
     */
    private FileUploadEvent parseEvent(Message message) {
        try {
            FileUploadEvent event = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), FileUploadEvent.class);
            
            // 验证关键字段
            if (event.getTaskId() == null || event.getTaskId().trim().isEmpty()) {
                throw new IllegalArgumentException("任务ID不能为空");
            }
            
            // 为空字段设置默认值
            if (event.getUploadedBytes() == null) {
                event.setUploadedBytes(0L);
            }
            if (event.getTotalBytes() == null && event.getFileSize() != null) {
                event.setTotalBytes(event.getFileSize());
            }
            if (event.getProgress() == null) {
                event.setProgress(0);
            }
            
            return event;
        } catch (Exception e) {
            log.error("❌ 解析文件上传事件失败 - 消息类型: {}, 消息载荷: {}, 错误: {}", 
                    message.getMessageType(), message.getPayload(), e.getMessage(), e);
            throw new RuntimeException("解析文件上传事件失败: " + e.getMessage(), e);
        }
    }


}
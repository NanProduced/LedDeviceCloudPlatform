package org.nan.cloud.file.infrastructure.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.common.mq.producer.MessageProducer;
import org.nan.cloud.common.mq.producer.SendResult;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.api.dto.FileUploadResponse;
import org.nan.cloud.file.application.service.FileUploadEventService;
import org.nan.cloud.file.application.service.TaskContextService;
import org.nan.cloud.file.application.domain.TaskContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传事件发布器
 * 
 * 职责：
 * 1. 将file-service的文件上传事件发布到消息队列
 * 2. 供core-service消费，创建MaterialFile和Material业务数据
 * 3. 触发上传进度和状态的消息推送给message-service
 * 
 * 遵循现有的Message格式和路由键规范
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadEventPublisher implements FileUploadEventService {

    private final MessageProducer messageProducer;
    private final TaskContextService taskContextService;

    // 交换机配置 - 遵循现有规范
    private static final String MESSAGE_TYPE = "task";
    private static final String STOMP_PUSH_EXCHANGE = "stomp.push.topic";
    private static final String BUSINESS_EXCHANGE = "business.topic";
    
    // 路由键模板 - 遵循message-service规范
    private static final String STOMP_ROUTING_KEY_TEMPLATE = "stomp.file.upload.%s.%s"; // {orgId}.{userId}
    private static final String BUSINESS_ROUTING_KEY_TEMPLATE = "file.upload.%s.%s.%s"; // {eventType}.{orgId}.{fileId}

    /**
     * 发布文件上传开始事件
     */
    @Override
    public void publishUploadStarted(String taskId, FileUploadRequest uploadRequest, 
                                   String originalFilename, Long fileSize, String organizationId) {
        try {

            String userId = uploadRequest.getUid().toString();


            // payload需要符合core-service中FileUploadEvent类字段
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "STARTED");
            payload.put("uploadType", "MATERIAL");
            payload.put("taskId", taskId);
            payload.put("originalFilename", originalFilename);
            payload.put("fileSize", fileSize);
            payload.put("progress", 0);
            payload.put("userId", userId);
            payload.put("organizationId", organizationId);
            payload.put("uploadStatus", "UPLOADING");
            payload.put("timestamp", LocalDateTime.now());

            // 发送到core-service构造相关数据
            // 这里需要和core-service中FileUploadEventListener实现一致
            publishBusinessMessage("FILE_UPLOAD_STARTED", payload, organizationId, userId, "文件上传开始");

            log.info("📤 文件上传开始事件已发布 - 任务ID: {}, 文件名: {}", taskId, originalFilename);
            
        } catch (Exception e) {
            log.error("❌ 发布文件上传开始事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件上传任务创建事件 - 异步上传专用
     */
    @Override
    public void publishUploadTaskCreated(String taskId, String fileId, FileUploadRequest uploadRequest,
                                       String originalFilename, Long fileSize, String organizationId) {
        try {
            String userId = uploadRequest.getUid().toString();

            // payload需要符合core-service中FileUploadEvent类字段
            // todo: 后续优化
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "CREATED");
            payload.put("uploadType", "MATERIAL");
            payload.put("taskId", taskId);
            payload.put("fileId", fileId);
            payload.put("materialName", uploadRequest.getMaterialName());
            payload.put("folderId", uploadRequest.getFolderId());
            payload.put("originalFilename", originalFilename);
            payload.put("fileSize", fileSize);
            payload.put("progress", 0);
            payload.put("userId", userId);
            payload.put("organizationId", organizationId);
            payload.put("userGroupId", uploadRequest.getUgid().toString());
            payload.put("uploadStatus", "UPLOADING");
            payload.put("timestamp", LocalDateTime.now());

            // 发送到core-service创建Task和Material实体
            publishBusinessMessage("FILE_UPLOAD_TASK_CREATED", payload, organizationId, fileId, "异步文件上传任务创建");

            log.info("📤 异步文件上传任务创建事件已发布 - 任务ID: {}, 文件ID: {}, 文件名: {}", taskId, fileId, originalFilename);
            
        } catch (Exception e) {
            log.error("❌ 发布异步文件上传任务创建事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件上传进度事件
     */
    @Override
    public void publishUploadProgress(String taskId, int progress, String status) {
        try {
            TaskContext taskContext = taskContextService.getTaskContext(taskId);
            if (taskContext != null) {
                // 计算已上传字节数
                long totalBytes = taskContext.getFileSize() != null ? taskContext.getFileSize() : 0L;
                long uploadedBytes = (long) (totalBytes * progress / 100.0);
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("eventType", "PROGRESS");
                payload.put("taskId", taskId);
                payload.put("progress", progress);
                payload.put("uploadedBytes", uploadedBytes);
                payload.put("totalBytes", totalBytes);
                payload.put("uploadStatus", status);
                payload.put("originalFilename", taskContext.getOriginalFilename());
                payload.put("timestamp", LocalDateTime.now());

                publishStompMessage("FILE_UPLOAD", payload, 
                        taskContext.getOid().toString(), 
                        taskContext.getUid().toString(), 
                        "文件上传进度");
            }

            log.debug("📊 文件上传进度事件已发布 - 任务ID: {}, 进度: {}%", taskId, progress);
            
        } catch (Exception e) {
            log.error("❌ 发布文件上传进度事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件上传进度事件（详细版本）
     */
    @Override
    public void publishUploadProgress(String taskId, int progress, String status, 
                                    long speed, long uploadedBytes, long totalBytes) {
        try {
            TaskContext taskContext = taskContextService.getTaskContext(taskId);
            if (taskContext != null) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("eventType", "PROGRESS");
                payload.put("taskId", taskId);
                payload.put("progress", progress);
                payload.put("uploadedBytes", uploadedBytes);
                payload.put("totalBytes", totalBytes);
                payload.put("speed", speed); // 传输速度（字节/秒）
                payload.put("uploadStatus", status);
                payload.put("originalFilename", taskContext.getOriginalFilename());
                payload.put("timestamp", LocalDateTime.now());

                publishStompMessage("FILE_UPLOAD", payload, 
                        taskContext.getOid().toString(), 
                        taskContext.getUid().toString(), 
                        "文件上传进度");
            }

            log.debug("📊 详细文件上传进度事件已发布 - 任务ID: {}, 进度: {}%, 速度: {}KB/s", 
                    taskId, progress, speed / 1024);
            
        } catch (Exception e) {
            log.error("❌ 发布详细文件上传进度事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件上传完成事件
     */
    @Override
    public void publishUploadCompleted(String taskId, FileUploadResponse uploadResponse, String organizationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "COMPLETED");
            payload.put("taskId", taskId);
            payload.put("fileId", uploadResponse.getFileId());
            payload.put("originalFilename", uploadResponse.getOriginalFilename());
            payload.put("fileSize", uploadResponse.getFileSize());
            payload.put("fileType", uploadResponse.getFileType());
            payload.put("mimeType", uploadResponse.getMimeType());
            payload.put("md5Hash", uploadResponse.getMd5Hash());
            payload.put("storagePath", uploadResponse.getStoragePath());
            payload.put("accessUrl", uploadResponse.getAccessUrl());
            payload.put("thumbnailUrl", uploadResponse.getThumbnailUrl());
            payload.put("progress", 100);
            payload.put("uploadStatus", "SUCCESS");
            payload.put("timestamp", LocalDateTime.now());

            // 1. 发送到core-service用于业务处理（创建Material等）
            // 这里的messageType要与core-service中FileUploadEventListener中声明的处理类型一致
            publishBusinessMessage("FILE_UPLOAD_COMPLETED", payload, organizationId, uploadResponse.getFileId(), "文件上传完成业务处理");
            
            // 2. 发送到message-service用于STOMP推送
            TaskContext taskContext = taskContextService.getTaskContext(taskId);
            if (taskContext != null) {
                // 这里的messageType要与message-service中FileUploadMessageProcessor声明的处理类型一致
                publishStompMessage("FILE_UPLOAD", payload, organizationId, 
                        taskContext.getUid().toString(), "文件上传完成");
            }
            
            log.info("✅ 文件上传完成事件已发布 - 任务ID: {}, 文件ID: {}", taskId, uploadResponse.getFileId());
            
        } catch (Exception e) {
            log.error("❌ 发布文件上传完成事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布缩略图生成完成事件
     */
    @Override
    public void publishThumbnailGenerated(String fileId, String primaryThumbnailPath, String organizationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "THUMBNAIL_GENERATED");
            payload.put("fileId", fileId);
            payload.put("primaryThumbnailPath", primaryThumbnailPath);
            payload.put("timestamp", LocalDateTime.now());

            // 发送到core-service用于更新Material表的thumbnail_path字段
            publishBusinessMessage("THUMBNAIL_GENERATED", payload, organizationId, fileId, "缩略图生成完成");
            
            log.info("✅ 缩略图生成完成事件已发布 - 文件ID: {}, 主缩略图路径: {}", fileId, primaryThumbnailPath);
            
        } catch (Exception e) {
            log.error("❌ 发布缩略图生成完成事件失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件处理完成事件
     */
    @Override
    public void publishProcessingCompleted(String taskId, String fileId, String metadataId, String organizationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "PROCESSING_COMPLETED");
            payload.put("taskId", taskId);
            payload.put("fileId", fileId);
            payload.put("metadataId", metadataId);
            payload.put("timestamp", LocalDateTime.now());

            // 发送到core-service用于更新MaterialFile表的meta_data_id字段
            publishBusinessMessage("FILE_PROCESSING_COMPLETED", payload, organizationId, fileId, "文件处理完成");
            
            log.info("✅ 文件处理完成事件已发布 - 任务ID: {}, 文件ID: {}, 元数据ID: {}", taskId, fileId, metadataId);
            
        } catch (Exception e) {
            log.error("❌ 发布文件处理完成事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 发布文件上传失败事件
     */
    @Override
    public void publishUploadFailed(String taskId, String errorMessage, String organizationId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", "FAILED");
            payload.put("taskId", taskId);
            payload.put("uploadStatus", "FAILED");
            payload.put("errorMessage", errorMessage);
            payload.put("timestamp", LocalDateTime.now());

            // 发送到message-service用于STOMP推送
            TaskContext taskContext = taskContextService.getTaskContext(taskId);
            if (taskContext != null) {
                publishStompMessage("FILE_UPLOAD", payload, organizationId, 
                        taskContext.getUid().toString(), "文件上传失败");
            }
            
            log.error("❌ 文件上传失败事件已发布 - 任务ID: {}, 错误: {}", taskId, errorMessage);
            
        } catch (Exception e) {
            log.error("❌ 发布文件上传失败事件失败 - 任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 发布STOMP推送消息
     */
    private void publishStompMessage(String messageType, Map<String, Object> payload, 
                                   String organizationId, String userId, String subject) {
        try {
            String routingKey = String.format(STOMP_ROUTING_KEY_TEMPLATE, organizationId, userId);
            
            Message message = Message.builder()
                    .messageType(messageType)
                    .subject(subject)
                    .payload(payload)
                    .senderId("file-service")
                    .receiverId(userId)
                    .organizationId(organizationId)
                    .exchange(STOMP_PUSH_EXCHANGE)
                    .routingKey(routingKey)
                    .priority(5)
                    .sourceSystem("file-service")
                    .targetSystem("message-service")
                    .build();

            SendResult result = messageProducer.send(message);
            if (result.isSuccess()) {
                log.debug("✅ STOMP消息发送成功 - 路由键: {}, 消息ID: {}", routingKey, result.getMessageId());
            } else {
                log.error("❌ STOMP消息发送失败 - 路由键: {}, 错误: {}", routingKey, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("💥 STOMP消息发送异常 - 错误: {}", e.getMessage(), e);
        }
    }

    /**
     * 发布业务处理消息
     */
    private void publishBusinessMessage(String messageType, Map<String, Object> payload, 
                                      String organizationId, String fileId, String subject) {
        try {
            String routingKey = String.format(BUSINESS_ROUTING_KEY_TEMPLATE, messageType.toLowerCase(), organizationId, fileId);
            
            Message message = Message.builder()
                    .messageType(messageType)
                    .subject(subject)
                    .payload(payload)
                    .senderId("file-service")
                    .receiverId("core-service")
                    .organizationId(organizationId)
                    .exchange(BUSINESS_EXCHANGE)
                    .routingKey(routingKey)
                    .priority(3)
                    .sourceSystem("file-service")
                    .targetSystem("core-service")
                    .build();

            SendResult result = messageProducer.send(message);
            if (result.isSuccess()) {
                log.debug("✅ 业务消息发送成功 - 路由键: {}, 消息ID: {}", routingKey, result.getMessageId());
            } else {
                log.error("❌ 业务消息发送失败 - 路由键: {}, 错误: {}", routingKey, result.getErrorMessage());
            }
            
        } catch (Exception e) {
            log.error("💥 业务消息发送异常 - 错误: {}", e.getMessage(), e);
        }
    }
}
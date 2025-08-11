package org.nan.cloud.core.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.core.domain.Task;
import org.nan.cloud.core.enums.TaskStatusEnum;
import org.nan.cloud.core.enums.TaskTypeEnum;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialDO;
import org.nan.cloud.core.infrastructure.repository.mysql.DO.MaterialFileDO;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialFileMapper;
import org.nan.cloud.core.infrastructure.repository.mysql.mapper.MaterialMapper;
import org.nan.cloud.core.infrastructure.task.TaskStatusHandler;
import org.nan.cloud.core.service.TaskService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingEventListener implements MessageConsumer {

    private final TaskStatusHandler taskStatusHandler;
    private final MaterialFileMapper materialFileMapper;
    private final MaterialMapper materialMapper;
    private final TaskService taskService;

    @Override
    public ConsumeResult consume(Message message) {
        try {
            switch (message.getMessageType()) {
                case "FILE_TRANSCODING_CREATED" -> { handleCreated(message); return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
                case "FILE_TRANSCODING_STARTED" -> { handleStarted(message); return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
                case "FILE_TRANSCODING_PROGRESS" -> { handleProgress(message); return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
                case "FILE_TRANSCODING_COMPLETED" -> { handleCompleted(message); return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
                case "FILE_TRANSCODING_FAILED" -> { handleFailed(message); return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
                default -> { return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0); }
            }
        } catch (Exception e) {
            log.error("❌ 处理转码事件失败: {}", e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), "CONSUME_EXCEPTION", e.getMessage(), e);
        }
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[]{
                "FILE_TRANSCODING_CREATED",
                "FILE_TRANSCODING_STARTED",
                "FILE_TRANSCODING_PROGRESS",
                "FILE_TRANSCODING_COMPLETED",
                "FILE_TRANSCODING_FAILED"
        };
    }

    @Override
    public String getConsumerId() {
        return "TranscodingEventListener";
    }

    private void handleCompleted(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) p.get("taskId");
        String targetFileId = (String) p.get("targetFileId");
        String sourceFileId = (String) p.get("sourceFileId");
        String transcodeDetailId = (String) p.get("transcodeDetailId");
        String metadataId = (String) p.get("metadataId");
        String storagePath = (String) p.get("storagePath");
        String mimeType = (String) p.get("mimeType");
        String fileExtension = (String) p.get("fileExtension");
        String md5Hash = (String) p.get("md5Hash");
        Long fileSize = toLong(p.get("fileSize"));
        Long oid = toLong(p.get("organizationId"));
        Long sourceMaterialId = toLong(p.get("sourceMaterialId"));
        String presetName = (String) p.get("presetName");

        // 1) 写入 material_file
        MaterialFileDO mf = MaterialFileDO.builder()
                .fileId(targetFileId)
                .md5Hash(md5Hash)
                .originalFileSize(fileSize)
                .mimeType(mimeType)
                .fileExtension(fileExtension)
                .storageType("LOCAL")
                .storagePath(storagePath)
                .uploadTime(LocalDateTime.now())
                .metaDataId(metadataId)
                .refCount(1L)
                .fileStatus(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .sourceFileId(sourceFileId)
                .transcodeDetailId(transcodeDetailId)
                .build();
        // 设置缩略图路径（如有）
        String thumbnailPath = (String) p.get("thumbnailPath");
        if (thumbnailPath != null && !thumbnailPath.isBlank()) {
            mf.setThumbnailPath(thumbnailPath);
        }
        materialFileMapper.insert(mf);

        // 2) 创建新 material（指向 targetFileId），命名：原名+格式，组/文件夹继承自原素材
        MaterialDO src = null;
        if (sourceMaterialId != null) {
            src = materialMapper.selectById(sourceMaterialId);
        }

        String baseName = (src != null && src.getMaterialName() != null) ? src.getMaterialName() : targetFileId;
        // 去除原名扩展
        String baseNameNoExt = baseName;
        int dot = baseName.lastIndexOf('.')
                ;
        if (dot > 0) {
            baseNameNoExt = baseName.substring(0, dot);
        }
        String fmt = (fileExtension != null && !fileExtension.isBlank()) ? fileExtension.toUpperCase() : "TRANSCODED";
        String newName = baseNameNoExt + "(" + fmt + ")";

        MaterialDO md = new MaterialDO();
        md.setMaterialName(newName);
        md.setFileId(targetFileId);
        md.setOid(oid);
        // 继承用户组与文件夹
        md.setUgid(src != null ? src.getUgid() : null);
        md.setFid(src != null ? src.getFid() : null);
        md.setMaterialType("VIDEO");
        md.setDescription(null);
        md.setUsageCount(0L);
        md.setUploadedBy(null);
        md.setCreateTime(LocalDateTime.now());
        md.setUpdateTime(LocalDateTime.now());
        md.setSourceMaterialId(sourceMaterialId);
        md.setTranscodePreset(presetName);
        materialMapper.insert(md);

        // 3) 完成任务
        taskStatusHandler.completeTask(taskId, null);
    }

    private void handleCreated(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) p.get("taskId");
        
        // 仅确认任务已接收，不重复创建Task（已在MaterialTranscodeController中创建）
        log.info("📨 转码任务已被file-service接收 - taskId={}", taskId);
        
        // 可选：更新任务状态为ACCEPTED，表明已被处理服务接收
        try {
            taskStatusHandler.updateTaskStatus(taskId, TaskStatusEnum.RUNNING);
            log.info("✅ 任务状态已更新为RUNNING - taskId={}", taskId);
        } catch (Exception e) {
            log.warn("⚠️ 更新任务状态失败，但不影响转码流程 - taskId={}, error={}", taskId, e.getMessage());
        }
    }

    private void handleStarted(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) p.get("taskId");
        taskStatusHandler.updateTaskStatus(taskId, org.nan.cloud.core.enums.TaskStatusEnum.RUNNING);
    }

    private void handleProgress(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) p.get("taskId");
        Integer progress = null;
        try { progress = p.get("progress") == null ? null : Integer.valueOf(String.valueOf(p.get("progress"))); } catch (Exception ignored) {}
        if (progress != null) {
            taskStatusHandler.updateTaskProgress(taskId, progress);
        }
    }

    private void handleFailed(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> p = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) p.get("taskId");
        String errorMessage = (String) p.get("errorMessage");
        taskStatusHandler.failTask(taskId, errorMessage);
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        try { return Long.valueOf(String.valueOf(v)); } catch (Exception e) { return null; }
    }
}


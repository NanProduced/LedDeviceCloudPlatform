package org.nan.cloud.file.infrastructure.mq.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.nan.cloud.common.mq.consumer.ConsumeResult;
import org.nan.cloud.common.mq.consumer.MessageConsumer;
import org.nan.cloud.common.mq.core.message.Message;
import org.nan.cloud.file.application.repository.TranscodingDetailRepository;
import org.nan.cloud.common.basic.domain.TranscodingDetail;
import org.nan.cloud.file.application.service.TaskContextService;
import org.nan.cloud.file.application.domain.TaskContext;
import org.nan.cloud.file.infrastructure.mq.producer.TranscodingEventPublisher;
import org.nan.cloud.file.infrastructure.repository.mysql.mapper.MaterialMapper;
import org.nan.cloud.file.infrastructure.repository.mysql.mapper.MaterialFileMapper;
import org.nan.cloud.file.infrastructure.repository.mysql.DO.MaterialFileDO;
import org.nan.cloud.file.infrastructure.transcoding.FFmpegTranscoder;
import org.nan.cloud.file.infrastructure.transcoding.FFmpegTranscoder.TranscodingConfig;
import org.nan.cloud.file.infrastructure.transcoding.FFmpegTranscoder.TranscodingResult;
import org.springframework.stereotype.Component;
 

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingTaskListener implements MessageConsumer {

    private final TranscodingDetailRepository transcodingDetailRepository;
    private final TaskContextService taskContextService;
    private final TranscodingEventPublisher eventPublisher;
    private final MaterialMapper materialMapper;
    private final MaterialFileMapper materialFileMapper;
    private final FFmpegTranscoder ffmpegTranscoder;

    @Override
    public ConsumeResult consume(Message message) {
        try {
            log.info("📥 收到转码任务消息 - 类型: {}", message.getMessageType());
            if ("FILE_TRANSCODING_CREATED".equals(message.getMessageType())) {
                handleTranscodingCreated(message);
                return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0);
            }
            return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0);
        } catch (Exception e) {
            log.error("❌ 处理转码任务消息失败: {}", e.getMessage(), e);
            return ConsumeResult.failure(message.getMessageId(), getConsumerId(), "CONSUME_EXCEPTION", e.getMessage(), e);
        }
    }

    @Override
    public String[] getSupportedMessageTypes() {
        return new String[]{"FILE_TRANSCODING_CREATED"};
    }

    @Override
    public String getConsumerId() {
        return "TranscodingTaskListener";
    }

    private void handleTranscodingCreated(Message message) {
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
        String taskId = (String) payload.get("taskId");
        Long oid = Long.valueOf((String) payload.get("organizationId"));
        Long uid = Long.valueOf((String) payload.get("userId"));
        Long sourceMaterialId = Long.valueOf(String.valueOf(payload.get("sourceMaterialId")));

        // 建立任务上下文（文件ID暂为空，占位）
        taskContextService.createTaskContext(taskId, null, uid, oid, "material-" + sourceMaterialId, 0L);
        taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.CREATED);

        // 初始化转码详情
        TranscodingDetail detail = TranscodingDetail.builder()
                .taskId(taskId)
                .oid(oid)
                .uid(uid)
                .sourceMaterialId(sourceMaterialId)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        String detailId = transcodingDetailRepository.save(detail);
        log.info("✅ 初始化转码详情完成 - taskId={}, detailId={}", taskId, detailId);
        // 查源文件信息并执行转码
        try {
            // 1) 从本地数据库查询源文件ID与路径
            String sourceFileId = materialMapper.selectFileIdByMaterialId(sourceMaterialId);
            if (sourceFileId == null) {
                throw new IllegalStateException("未找到源素材对应的文件ID");
            }
            MaterialFileDO srcFile = materialFileMapper.selectById(sourceFileId);
            if (srcFile == null || srcFile.getStoragePath() == null) {
                throw new IllegalStateException("未找到源文件或存储路径为空");
            }

            // 2) 标记开始并推进度
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.PROCESSING);
            eventPublisher.publishTranscodingStarted(taskId, oid, uid, sourceMaterialId);

            // 3) 规划输出路径（与上传保持一致的日期分区 + transcoded 子目录）
            String outputFileId = java.util.UUID.randomUUID().toString();
            String fileExtension = "mp4"; // 简化：默认输出mp4，可按参数覆盖
            String datePart = java.time.LocalDate.now().toString();
            Path outputTemp = Files.createTempFile("transcoded-", "." + fileExtension);

            // 4) 执行转码（简化参数，可扩展从payload中读取）
            TranscodingConfig config = TranscodingConfig.builder()
                    .videoCodec("libx264")
                    .audioCodec("aac")
                    .crf(23)
                    .preset("medium")
                    .build();
            TranscodingResult result = ffmpegTranscoder.transcode(
                    srcFile.getStoragePath(), outputTemp.toString(), config,
                    (progress, fps, currentTime) -> {
                        try { taskContextService.updateTaskProgress(taskId, progress); } catch (Exception ignored) {}
                        try { eventPublisher.publishTranscodingProgress(taskId, progress, "TRANSCODING"); } catch (Exception ignored) {}
                    }
            );

            if (!result.isSuccess()) {
                String err = result.getErrorMessage() != null ? result.getErrorMessage() : "转码失败";
                TranscodingDetail failed = TranscodingDetail.builder().id(detailId).status("FAILED").errorMessage(err).build();
                transcodingDetailRepository.update(failed);
                eventPublisher.publishTranscodingFailed(taskId, oid, uid, sourceMaterialId, err, detailId);
                taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.FAILED);
                return;
            }

            // 5) 生成最终存储路径，移动文件
            String finalStoragePath = String.format("/data/material/%s/transcoded/%s.%s", datePart, outputFileId, fileExtension);
            Path finalPath = Path.of(finalStoragePath);
            Files.createDirectories(finalPath.getParent());
            Files.move(Path.of(result.getOutputPath()), finalPath, StandardCopyOption.REPLACE_EXISTING);

            // 6) 计算MD5与文件大小
            String md5 = calculateMd5(finalPath);
            long fileSize = Files.size(finalPath);

            // 7) TODO: 生成缩略图、写入Mongo元数据，示例中直接占位
            String metadataId = null; // TODO 填充真实元数据
            String thumbnailPath = null;

            // 8) 更新转码详情并发布完成事件
            TranscodingDetail completed = TranscodingDetail.builder()
                    .id(detailId)
                    .targetFileId(outputFileId)
                    .sourceFileId(srcFile.getFileId())
                    .parameters(java.util.Map.of("md5", md5, "fileSize", fileSize))
                    .status("COMPLETED")
                    .completedAt(LocalDateTime.now())
                    .build();
            transcodingDetailRepository.update(completed);
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.COMPLETED);
            taskContextService.updateTaskProgress(taskId, 100);
            eventPublisher.publishTranscodingCompleted(
                    taskId, oid, uid, sourceMaterialId,
                    sourceFileId, outputFileId, finalStoragePath,
                    "video/mp4", fileSize, fileExtension,
                    metadataId, thumbnailPath, (String) payload.get("presetName"), detailId
            );

        } catch (Exception ex) {
            log.error("转码执行异常 - taskId={}", taskId, ex);
            TranscodingDetail failedEx = TranscodingDetail.builder().id(detailId).status("FAILED").errorMessage(ex.getMessage()).build();
            transcodingDetailRepository.update(failedEx);
            eventPublisher.publishTranscodingFailed(taskId, oid, uid, sourceMaterialId, ex.getMessage(), detailId);
            taskContextService.updateTaskStatus(taskId, TaskContext.TaskStatus.FAILED);
        }
    }

    private String calculateMd5(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (var in = Files.newInputStream(file)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                md.update(buf, 0, r);
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }
}


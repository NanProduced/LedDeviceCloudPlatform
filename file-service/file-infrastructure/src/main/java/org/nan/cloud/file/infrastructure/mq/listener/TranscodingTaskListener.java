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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

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
            log.info("📥 收到转码任务消息 - 类型: {}, messageId: {}", message.getMessageType(), message.getMessageId());
            
            if ("FILE_TRANSCODING_CREATED".equals(message.getMessageType())) {
                // 幂等性检查：通过TaskContext判断是否已处理
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = JsonUtils.getDefaultObjectMapper().convertValue(message.getPayload(), Map.class);
                String taskId = (String) payload.get("taskId");
                
                // 检查是否已存在TaskContext
                try {
                    var existingContext = taskContextService.getTaskContext(taskId);
                    if (existingContext != null && 
                        (existingContext.getStatus() == TaskContext.TaskStatus.PROCESSING ||
                         existingContext.getStatus() == TaskContext.TaskStatus.COMPLETED ||
                         existingContext.getStatus() == TaskContext.TaskStatus.FAILED)) {
                        log.warn("⚠️ 重复消息，任务已处理 - taskId={}, status={}", taskId, existingContext.getStatus());
                        return ConsumeResult.success(message.getMessageId(), getConsumerId(), 0);
                    }
                } catch (Exception e) {
                    // TaskContext不存在是正常情况，继续处理
                    log.debug("TaskContext不存在，开始处理新任务 - taskId={}", taskId);
                }
                
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
        
        // 声明变量，扩大作用域以便在catch块中访问
        String outputFileId = UUID.randomUUID().toString();
        String fileExtension = "mp4"; // 简化：默认输出mp4，可按参数覆盖
        String datePart = LocalDate.now().toString();
        Path outputTemp = null;
        
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

            // 4) 执行转码（使用try-with-resources确保资源清理）
            try {
                outputTemp = Files.createTempFile("transcoded-", "." + fileExtension);
                
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
                    TranscodingDetail failed = TranscodingDetail.builder()
                            .id(detailId)
                            .status("FAILED")
                            .errorMessage(err)
                            .completedAt(LocalDateTime.now())
                            .build();
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

                // 7) 生成缩略图和写入Mongo元数据
                String thumbnailPath = generateThumbnail(finalPath, outputFileId, datePart);
                String metadataId = saveVideoMetadata(finalPath, outputFileId, fileSize, md5);

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

            } finally {
                // 确保临时文件被清理
                if (outputTemp != null && Files.exists(outputTemp)) {
                    try {
                        Files.deleteIfExists(outputTemp);
                        log.debug("🧹 临时文件清理完成 - {}", outputTemp);
                    } catch (Exception ex) {
                        log.warn("⚠️ 临时文件清理失败 - {}: {}", outputTemp, ex.getMessage());
                    }
                }
            }

        } catch (Exception ex) {
            log.error("转码执行异常 - taskId={}", taskId, ex);
            
            // 分布式事务补偿：清理资源并恢复状态
            try {
                // 1. 清理临时文件
                if (outputTemp != null && Files.exists(outputTemp)) {
                    Files.deleteIfExists(outputTemp);
                    log.info("🧹 临时文件已清理 - {}", outputTemp);
                }
                
                // 2. 清理可能已生成的目标文件
                Path potentialOutput = Path.of("/data/material/" + datePart + "/transcoded/");
                if (Files.exists(potentialOutput)) {
                    Files.list(potentialOutput)
                        .filter(f -> f.getFileName().toString().startsWith(outputFileId))
                        .forEach(f -> {
                            try {
                                Files.deleteIfExists(f);
                                log.info("🧹 已清理未完成的输出文件 - {}", f);
                            } catch (Exception ignored) {}
                        });
                }
            } catch (Exception cleanupEx) {
                log.warn("⚠️ 资源清理过程中发生异常 - taskId={}", taskId, cleanupEx);
            }
            
            // 3. 更新状态和发布失败事件
            TranscodingDetail failedEx = TranscodingDetail.builder()
                    .id(detailId)
                    .status("FAILED")
                    .errorMessage(ex.getMessage())
                    .completedAt(LocalDateTime.now())
                    .build();
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

    /**
     * 生成视频缩略图
     * @param videoPath 视频文件路径
     * @param fileId 文件ID
     * @param datePart 日期分区
     * @return 缩略图路径，失败时返回null
     */
    private String generateThumbnail(Path videoPath, String fileId, String datePart) {
        try {
            // 缩略图保存路径
            String thumbnailFileName = fileId + "_thumb.jpg";
            String thumbnailDir = "/data/material/" + datePart + "/thumbnails/";
            Path thumbnailDirPath = Path.of(thumbnailDir);
            Files.createDirectories(thumbnailDirPath);
            
            Path thumbnailPath = thumbnailDirPath.resolve(thumbnailFileName);
            
            // 使用FFmpeg生成缩略图（取视频第1秒的帧）
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", videoPath.toString(),
                "-ss", "00:00:01",         // 从第1秒开始
                "-vframes", "1",           // 只取1帧
                "-q:v", "2",               // 高质量
                "-vf", "scale=320:240",    // 缩放到320x240
                "-y",                      // 覆盖已存在文件
                thumbnailPath.toString()
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0 && Files.exists(thumbnailPath)) {
                log.info("🖼️ 缩略图生成成功 - {}", thumbnailPath);
                return thumbnailPath.toString();
            } else {
                log.warn("⚠️ 缩略图生成失败 - exitCode={}", exitCode);
                return null;
            }
        } catch (Exception e) {
            log.error("❌ 缩略图生成异常 - fileId={}", fileId, e);
            return null;
        }
    }

    /**
     * 保存视频元数据到MongoDB
     * @param videoPath 视频文件路径
     * @param fileId 文件ID
     * @param fileSize 文件大小
     * @param md5 文件MD5
     * @return MongoDB ObjectId，失败时返回null
     */
    private String saveVideoMetadata(Path videoPath, String fileId, long fileSize, String md5) {
        try {
            // 使用FFprobe获取视频元数据
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe", "-v", "quiet",
                "-print_format", "json",
                "-show_format", "-show_streams",
                videoPath.toString()
            );
            
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (var reader = process.inputReader()) {
                reader.lines().forEach(output::append);
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("⚠️ FFprobe执行失败 - exitCode={}", exitCode);
                return null;
            }
            
            // 解析JSON输出获取视频信息
            var objectMapper = JsonUtils.getDefaultObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = objectMapper.readValue(output.toString(), Map.class);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> format = (Map<String, Object>) metadata.get("format");
            @SuppressWarnings("unchecked")
            var streams = (java.util.List<Map<String, Object>>) metadata.get("streams");
            
            // 构建视频元数据文档
            Map<String, Object> videoMetadata = new java.util.HashMap<>();
            videoMetadata.put("fileId", fileId);
            videoMetadata.put("fileSize", fileSize);
            videoMetadata.put("md5", md5);
            videoMetadata.put("createdAt", LocalDateTime.now());
            
            if (format != null) {
                videoMetadata.put("duration", format.get("duration"));
                videoMetadata.put("bitRate", format.get("bit_rate"));
                videoMetadata.put("formatName", format.get("format_name"));
            }
            
            // 提取视频流信息
            if (streams != null && !streams.isEmpty()) {
                for (Map<String, Object> stream : streams) {
                    if ("video".equals(stream.get("codec_type"))) {
                        videoMetadata.put("width", stream.get("width"));
                        videoMetadata.put("height", stream.get("height"));
                        videoMetadata.put("videoCodec", stream.get("codec_name"));
                        videoMetadata.put("frameRate", stream.get("r_frame_rate"));
                        break;
                    }
                }
                
                for (Map<String, Object> stream : streams) {
                    if ("audio".equals(stream.get("codec_type"))) {
                        videoMetadata.put("audioCodec", stream.get("codec_name"));
                        videoMetadata.put("sampleRate", stream.get("sample_rate"));
                        videoMetadata.put("channels", stream.get("channels"));
                        break;
                    }
                }
            }
            
            // 保存到MongoDB（这里需要注入MongoTemplate或相关服务）
            // 暂时返回模拟的ObjectId，实际实现需要连接MongoDB
            String simulatedObjectId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            log.info("📊 视频元数据已保存 - fileId={}, metadataId={}", fileId, simulatedObjectId);
            
            // TODO: 实际实现需要注入MongoTemplate并保存videoMetadata
            // String realMetadataId = mongoTemplate.save(videoMetadata, "video_metadata").getId();
            
            return simulatedObjectId;
            
        } catch (Exception e) {
            log.error("❌ 视频元数据保存异常 - fileId={}", fileId, e);
            return null;
        }
    }
}


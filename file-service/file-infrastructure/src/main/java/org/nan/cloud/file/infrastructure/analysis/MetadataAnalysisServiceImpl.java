package org.nan.cloud.file.infrastructure.analysis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.service.MetadataAnalysisService;
import org.nan.cloud.file.application.service.StorageService;
import org.nan.cloud.common.basic.domain.MaterialMetadata;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

// FFmpeg相关导入
import org.bytedeco.javacv.FFmpegFrameGrabber;

/**
 * 文件元数据分析服务实现 - 适配统一模型
 * 
 * 使用Apache Tika和FFmpeg进行深度文件分析
 * 适配统一的MaterialMetadata模型结构
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataAnalysisServiceImpl implements MetadataAnalysisService {

    private final StorageService storageService;
    private final Tika tika = new Tika();

    // 支持的MIME类型
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", 
            "image/tiff", "image/webp"
    );

    private static final Set<String> SUPPORTED_VIDEO_TYPES = Set.of(
            "video/mp4", "video/avi", "video/mov", "video/wmv", "video/flv", 
            "video/mkv", "video/webm", "video/3gp"
    );

    private static final Set<String> SUPPORTED_AUDIO_TYPES = Set.of(
            "audio/mp3", "audio/wav", "audio/flac", "audio/aac", "audio/ogg", 
            "audio/wma", "audio/m4a"
    );

    private static final Set<String> SUPPORTED_DOCUMENT_TYPES = Set.of(
            "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain", "text/html", "text/xml"
    );

    @Override
    public MaterialMetadata analyzeMetadata(FileInfo fileInfo, String taskId) {
        log.info("开始分析文件元数据 - 文件ID: {}, 任务ID: {}", fileInfo.getFileId(), taskId);
        
        try {
            String filePath = storageService.getAbsolutePath(fileInfo.getStoragePath());
            File file = new File(filePath);
            
            if (!file.exists()) {
                log.warn("文件不存在，无法分析元数据: {}", filePath);
                return createFailedMetadata(fileInfo, taskId, "文件不存在");
            }

            // 构建基础文件信息
            MaterialMetadata.FileBasicInfo basicInfo = MaterialMetadata.FileBasicInfo.builder()
                    .fileName(fileInfo.getOriginalFilename())
                    .mimeType(fileInfo.getMimeType())
                    .fileType(getFileTypeCategory(fileInfo.getMimeType()))
                    .fileFormat(getFileFormat(fileInfo.getOriginalFilename()))
                    .fileExtension(getFileExtension(fileInfo.getOriginalFilename()))
                    .fileSize(fileInfo.getFileSize())
                    .md5Hash(fileInfo.getMd5Hash())
                    .build();

            // 构建MaterialMetadata
            MaterialMetadata.MaterialMetadataBuilder builder = MaterialMetadata.builder()
                    .fileId(fileInfo.getFileId())
                    .originalFilename(fileInfo.getOriginalFilename())
                    .analysisTaskId(taskId)
                    .basicInfo(basicInfo)
                    .analysisStatus("ANALYZING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());

            // 根据文件类型进行不同的分析
            String fileType = getFileTypeCategory(fileInfo.getMimeType());
            switch (fileType) {
                case "IMAGE":
                    analyzeImageMetadata(builder, file, fileInfo);
                    break;
                case "VIDEO":
                    analyzeVideoMetadata(builder, file, fileInfo);
                    break;
                case "AUDIO":
                    analyzeAudioMetadata(builder, file, fileInfo);
                    break;
                case "DOCUMENT":
                    analyzeDocumentMetadata(builder, file, fileInfo);
                    break;
                default:
                    log.debug("文件类型 {} 不需要特殊分析", fileType);
            }

            // 使用Tika进行通用元数据提取
            extractTikaMetadata(builder, file);

            MaterialMetadata metadata = builder
                    .analysisStatus("COMPLETED")
                    .updatedAt(LocalDateTime.now())
                    .build();

            log.info("✅ 文件元数据分析完成 - 文件ID: {}", fileInfo.getFileId());
            return metadata;

        } catch (Exception e) {
            log.error("❌ 文件元数据分析失败 - 文件ID: {}, 错误: {}", fileInfo.getFileId(), e.getMessage(), e);
            return createFailedMetadata(fileInfo, taskId, e.getMessage());
        }
    }

    /**
     * 分析图片元数据
     */
    private void analyzeImageMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                MaterialMetadata.ImageMetadata imageMetadata = MaterialMetadata.ImageMetadata.builder()
                        .width(image.getWidth())
                        .height(image.getHeight())
                        .colorDepth(image.getColorModel().getPixelSize())
                        .colorSpace(image.getColorModel().getColorSpace().getType() == java.awt.color.ColorSpace.TYPE_RGB ? "RGB" : "OTHER")
                        .hasAlpha(image.getColorModel().hasAlpha())
                        .build();

                // 尝试提取EXIF信息
                MaterialMetadata.ExifInfo exifInfo = extractExifInfo(file);
                if (exifInfo != null) {
                    imageMetadata.setExifInfo(exifInfo);
                }

                builder.imageMetadata(imageMetadata);
                log.debug("图片元数据分析完成 - 尺寸: {}x{}", image.getWidth(), image.getHeight());
            }
        } catch (Exception e) {
            log.warn("图片元数据分析失败: {}", e.getMessage());
        }
    }

    /**
     * 分析视频元数据
     */
    private void analyzeVideoMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();

            MaterialMetadata.VideoMetadata.VideoMetadataBuilder videoBuilder = MaterialMetadata.VideoMetadata.builder()
                    .videoWidth(grabber.getImageWidth())
                    .videoHeight(grabber.getImageHeight())
                    .videoDuration((long) (grabber.getLengthInTime() / 1000000.0)) // 转换为秒
                    .frameRate(grabber.getVideoFrameRate())
                    .videoBitrate((long) grabber.getVideoBitrate())
                    .videoCodec(grabber.getVideoCodecName())
                    .containerFormat(grabber.getFormat())
                    .aspectRatio(calculateAspectRatio(grabber.getImageWidth(), grabber.getImageHeight()));

            // 音频流信息
            if (grabber.getAudioChannels() > 0) {
                MaterialMetadata.AudioStreamInfo audioStream = MaterialMetadata.AudioStreamInfo.builder()
                        .audioCodec(grabber.getAudioCodecName())
                        .audioBitrate((long) grabber.getAudioBitrate())
                        .sampleRate(grabber.getSampleRate())
                        .channels(grabber.getAudioChannels())
                        .audioDuration((long) (grabber.getLengthInTime() / 1000000.0))
                        .build();
                videoBuilder.audioStream(audioStream);
            }

            builder.videoMetadata(videoBuilder.build());
            log.debug("视频元数据分析完成 - 尺寸: {}x{}, 时长: {}秒", 
                    grabber.getImageWidth(), grabber.getImageHeight(), 
                    grabber.getLengthInTime() / 1000000.0);

            grabber.stop();
        } catch (Exception e) {
            log.warn("视频元数据分析失败: {}", e.getMessage());
        }
    }

    /**
     * 分析音频元数据
     */
    private void analyzeAudioMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();

            MaterialMetadata.AudioMetadata audioMetadata = MaterialMetadata.AudioMetadata.builder()
                    .audioDuration((long) (grabber.getLengthInTime() / 1000000.0))
                    .audioBitrate((long) grabber.getAudioBitrate())
                    .sampleRate(grabber.getSampleRate())
                    .channels(grabber.getAudioChannels())
                    .audioCodec(grabber.getAudioCodecName())
                    .containerFormat(grabber.getFormat())
                    .build();

            builder.audioMetadata(audioMetadata);
            log.debug("音频元数据分析完成 - 时长: {}秒, 比特率: {}", 
                    grabber.getLengthInTime() / 1000000.0, grabber.getAudioBitrate());

            grabber.stop();
        } catch (Exception e) {
            log.warn("音频元数据分析失败: {}", e.getMessage());
        }
    }

    /**
     * 分析文档元数据
     */
    private void analyzeDocumentMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (InputStream inputStream = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);

            parser.parse(inputStream, handler, metadata);

            MaterialMetadata.DocumentMetadata documentMetadata = MaterialMetadata.DocumentMetadata.builder()
                    .documentTitle(metadata.get("title"))
                    .documentAuthor(metadata.get("Author"))
                    .documentSubject(metadata.get("subject"))
                    .documentKeywords(metadata.get("Keywords"))
                    .creator(metadata.get("Application-Name"))
                    .build();

            // 尝试解析页数
            String pageCountStr = metadata.get("xmpTPg:NPages");
            if (pageCountStr != null) {
                try {
                    documentMetadata.setPageCount(Integer.parseInt(pageCountStr));
                } catch (NumberFormatException e) {
                    log.debug("无法解析页数: {}", pageCountStr);
                }
            }

            builder.documentMetadata(documentMetadata);
            log.debug("文档元数据分析完成 - 标题: {}, 作者: {}", 
                    documentMetadata.getDocumentTitle(), documentMetadata.getDocumentAuthor());

        } catch (Exception e) {
            log.warn("文档元数据分析失败: {}", e.getMessage());
        }
    }

    /**
     * 提取Tika通用元数据
     */
    private void extractTikaMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            tika.parse(inputStream, metadata);

            Map<String, Object> additionalProperties = new HashMap<>();
            for (String name : metadata.names()) {
                String value = metadata.get(name);
                if (StringUtils.hasText(value)) {
                    additionalProperties.put(name, value);
                }
            }

            if (!additionalProperties.isEmpty()) {
                // 将额外属性添加到basicInfo中
                MaterialMetadata.FileBasicInfo basicInfo = builder.build().getBasicInfo();
                if (basicInfo != null) {
                    basicInfo.setAdditionalProperties(additionalProperties);
                }
            }

        } catch (Exception e) {
            log.debug("Tika元数据提取失败: {}", e.getMessage());
        }
    }

    /**
     * 提取EXIF信息（简化版本）
     */
    private MaterialMetadata.ExifInfo extractExifInfo(File file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                IIOMetadata metadata = reader.getImageMetadata(0);
                
                if (metadata != null) {
                    // 这里可以实现详细的EXIF解析
                    // 暂时返回基础的EXIF信息结构
                    return MaterialMetadata.ExifInfo.builder()
                            .build();
                }
            }
        } catch (Exception e) {
            log.debug("EXIF信息提取失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 辅助方法 ====================

    private MaterialMetadata createFailedMetadata(FileInfo fileInfo, String taskId, String errorMessage) {
        MaterialMetadata.FileBasicInfo basicInfo = MaterialMetadata.FileBasicInfo.builder()
                .fileName(fileInfo.getOriginalFilename())
                .mimeType(fileInfo.getMimeType())
                .fileType(getFileTypeCategory(fileInfo.getMimeType()))
                .fileFormat(getFileFormat(fileInfo.getOriginalFilename()))
                .fileSize(fileInfo.getFileSize())
                .md5Hash(fileInfo.getMd5Hash())
                .build();

        return MaterialMetadata.builder()
                .fileId(fileInfo.getFileId())
                .originalFilename(fileInfo.getOriginalFilename())
                .analysisTaskId(taskId)
                .basicInfo(basicInfo)
                .analysisStatus("FAILED")
                .analysisError(errorMessage)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean isSupported(String mimeType) {
        return SUPPORTED_IMAGE_TYPES.contains(mimeType) ||
               SUPPORTED_VIDEO_TYPES.contains(mimeType) ||
               SUPPORTED_AUDIO_TYPES.contains(mimeType) ||
               SUPPORTED_DOCUMENT_TYPES.contains(mimeType);
    }

    @Override
    public String getFileTypeCategory(String mimeType) {
        if (mimeType == null) {
            return "DOCUMENT";
        }

        if (mimeType.startsWith("image/")) {
            return "IMAGE";
        } else if (mimeType.startsWith("video/")) {
            return "VIDEO";
        } else if (mimeType.startsWith("audio/")) {
            return "AUDIO";
        } else {
            return "DOCUMENT";
        }
    }

    private String getFileFormat(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1).toLowerCase() : "";
    }

    private String getFileExtension(String filename) {
        return getFileFormat(filename);
    }

    private String calculateAspectRatio(int width, int height) {
        if (height == 0) return "Unknown";
        
        double ratio = (double) width / height;
        if (Math.abs(ratio - 16.0/9.0) < 0.01) return "16:9";
        if (Math.abs(ratio - 4.0/3.0) < 0.01) return "4:3";
        if (Math.abs(ratio - 1.0) < 0.01) return "1:1";
        
        return String.format("%.2f:1", ratio);
    }
}
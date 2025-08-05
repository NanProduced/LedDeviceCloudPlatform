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
import org.nan.cloud.file.application.domain.MaterialMetadata;
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
 * 文件元数据分析服务实现
 * 
 * 使用Apache Tika和FFmpeg进行深度文件分析
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
            MaterialMetadata.MaterialMetadataBuilder builder = MaterialMetadata.builder()
                    .fileId(fileInfo.getFileId())
                    .originalFilename(fileInfo.getOriginalFilename())
                    .fileSize(fileInfo.getFileSize())
                    .mimeType(fileInfo.getMimeType())
                    .fileType(getFileTypeCategory(fileInfo.getMimeType()))
                    .fileFormat(getFileFormat(fileInfo.getOriginalFilename()))
                    .md5Hash(fileInfo.getMd5Hash())
                    .analysisTaskId(taskId)
                    .analysisStatus("ANALYZING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());

            String filePath = storageService.getAbsolutePath(fileInfo.getStoragePath());
            File file = new File(filePath);
            
            if (!file.exists()) {
                log.warn("文件不存在，无法分析元数据: {}", filePath);
                return builder.analysisStatus("FAILED")
                        .analysisError("文件不存在")
                        .build();
            }

            // 根据文件类型进行不同的分析
            String fileType = getFileTypeCategory(fileInfo.getMimeType());
            switch (fileType) {
                case "IMAGE":
                    analyzeImageProperties(builder, file, fileInfo);
                    break;
                case "VIDEO":
                    analyzeVideoProperties(builder, file, fileInfo);
                    break;
                case "AUDIO":
                    analyzeAudioProperties(builder, file, fileInfo);
                    break;
                case "DOCUMENT":
                    analyzeDocumentProperties(builder, file, fileInfo);
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
            return MaterialMetadata.builder()
                    .fileId(fileInfo.getFileId())
                    .originalFilename(fileInfo.getOriginalFilename())
                    .fileSize(fileInfo.getFileSize())
                    .mimeType(fileInfo.getMimeType())
                    .fileType(getFileTypeCategory(fileInfo.getMimeType()))
                    .md5Hash(fileInfo.getMd5Hash())
                    .analysisTaskId(taskId)
                    .analysisStatus("FAILED")
                    .analysisError(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * 分析图片属性
     */
    private void analyzeImageProperties(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                builder.imageWidth(image.getWidth())
                        .imageHeight(image.getHeight())
                        .colorDepth(image.getColorModel().getPixelSize());

                // 提取EXIF信息
                extractExifData(builder, file);
            }
        } catch (Exception e) {
            log.warn("图片属性分析失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * 分析视频属性
     */
    private void analyzeVideoProperties(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            
            builder.videoWidth(grabber.getImageWidth())
                    .videoHeight(grabber.getImageHeight())
                    .videoDuration((long) (grabber.getLengthInTime() / 1000000)) // 转换为秒
                    .frameRate(grabber.getVideoFrameRate())
                    .videoCodec(grabber.getVideoCodecName())
                    .videoBitrate((long) grabber.getVideoBitrate())
                    .aspectRatio(String.format("%.2f:1", (double) grabber.getImageWidth() / grabber.getImageHeight()));

            // 音频信息
            if (grabber.getAudioChannels() > 0) {
                builder.channels(grabber.getAudioChannels())
                        .sampleRate(grabber.getSampleRate())
                        .audioCodec(grabber.getAudioCodecName())
                        .audioBitrate((long) grabber.getAudioBitrate())
                        .audioDuration((long) (grabber.getLengthInTime() / 1000000));
            }
            
            grabber.stop();
        } catch (Exception e) {
            log.warn("视频属性分析失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * 分析音频属性
     */
    private void analyzeAudioProperties(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file)) {
            grabber.start();
            
            builder.channels(grabber.getAudioChannels())
                    .sampleRate(grabber.getSampleRate())
                    .audioCodec(grabber.getAudioCodecName())
                    .audioBitrate((long) grabber.getAudioBitrate())
                    .audioDuration((long) (grabber.getLengthInTime() / 1000000));
            
            grabber.stop();
        } catch (Exception e) {
            log.warn("音频属性分析失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * 分析文档属性
     */
    private void analyzeDocumentProperties(MaterialMetadata.MaterialMetadataBuilder builder, File file, FileInfo fileInfo) {
        try (InputStream stream = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            
            parser.parse(stream, handler, metadata);
            
            // 提取文档属性
            builder.documentTitle(metadata.get("title"))
                    .documentAuthor(metadata.get("creator"))
                    .documentSubject(metadata.get("subject"))
                    .documentKeywords(metadata.get("keywords"));

            // 尝试获取页数（主要针对PDF）
            String pages = metadata.get("xmpTPg:NPages");
            if (pages == null) {
                pages = metadata.get("meta:page-count");  
            }
            if (pages != null) {
                try {
                    builder.pageCount(Integer.parseInt(pages));
                } catch (NumberFormatException e) {
                    log.debug("页数解析失败: {}", pages);
                }
            }

            // 提取创建和修改时间
            String created = metadata.get("dcterms:created");
            String modified = metadata.get("dcterms:modified");
            
            if (created != null) {
                try {
                    builder.documentCreated(LocalDateTime.parse(created));
                } catch (Exception e) {
                    log.debug("文档创建时间解析失败: {}", created);
                }
            }
            
            if (modified != null) {
                try {
                    builder.documentModified(LocalDateTime.parse(modified));
                } catch (Exception e) {
                    log.debug("文档修改时间解析失败: {}", modified);
                }
            }

        } catch (Exception e) {
            log.warn("文档属性分析失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * 使用Tika提取通用元数据
     */
    private void extractTikaMetadata(MaterialMetadata.MaterialMetadataBuilder builder, File file) {
        try (InputStream stream = new FileInputStream(file)) {
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            
            parser.parse(stream, handler, metadata);
            
            // 构建额外属性Map
            Map<String, Object> additionalProperties = new HashMap<>();
            for (String name : metadata.names()) {
                additionalProperties.put(name, metadata.get(name));
            }
            
            builder.additionalProperties(additionalProperties);
            
        } catch (Exception e) {
            log.debug("Tika元数据提取失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
    }

    /**
     * 提取图片EXIF数据
     */
    private void extractExifData(MaterialMetadata.MaterialMetadataBuilder builder, File file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis, true);
                
                IIOMetadata metadata = reader.getImageMetadata(0);
                if (metadata != null) {
                    // 简化的EXIF提取，只获取关键信息
                    String[] formatNames = metadata.getMetadataFormatNames();
                    StringBuilder exifBuilder = new StringBuilder();
                    
                    for (String formatName : formatNames) {
                        exifBuilder.append(formatName).append(": ");
                        try {
                            exifBuilder.append(metadata.getAsTree(formatName).toString());
                        } catch (Exception e) {
                            exifBuilder.append("解析失败");
                        }
                        exifBuilder.append("; ");
                    }
                    
                    if (exifBuilder.length() > 0) {
                        builder.exifData(exifBuilder.toString());
                    }
                }
                
                reader.dispose();
            }
        } catch (Exception e) {
            log.debug("EXIF数据提取失败: {}, 错误: {}", file.getName(), e.getMessage());
        }
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
        if (SUPPORTED_IMAGE_TYPES.contains(mimeType)) {
            return "IMAGE";
        } else if (SUPPORTED_VIDEO_TYPES.contains(mimeType)) {
            return "VIDEO";
        } else if (SUPPORTED_AUDIO_TYPES.contains(mimeType)) {
            return "AUDIO";
        } else if (SUPPORTED_DOCUMENT_TYPES.contains(mimeType)) {
            return "DOCUMENT";
        } else {
            return "OTHER";
        }
    }

    /**
     * 从文件名获取文件格式
     */
    private String getFileFormat(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return null;
    }
}
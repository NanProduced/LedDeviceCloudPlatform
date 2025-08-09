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
                // 🔍 精确的透明度检测
                boolean hasActualTransparency = detectActualTransparency(image, fileInfo.getMimeType());
                
                MaterialMetadata.ImageMetadata.ImageMetadataBuilder imageBuilder = MaterialMetadata.ImageMetadata.builder()
                        .width(image.getWidth())
                        .height(image.getHeight())
                        .colorDepth(image.getColorModel().getPixelSize())
                        .colorSpace(image.getColorModel().getColorSpace().getType() == java.awt.color.ColorSpace.TYPE_RGB ? "RGB" : "OTHER")
                        .hasAlpha(hasActualTransparency);

                // 🎯 GIF动画特殊处理
                if ("image/gif".equalsIgnoreCase(fileInfo.getMimeType())) {
                    analyzeGifAnimation(imageBuilder, file);
                }

                MaterialMetadata.ImageMetadata imageMetadata = imageBuilder.build();

                // 尝试提取EXIF信息
                MaterialMetadata.ExifInfo exifInfo = extractExifInfo(file);
                if (exifInfo != null) {
                    imageMetadata.setExifInfo(exifInfo);
                }

                builder.imageMetadata(imageMetadata);
                log.debug("图片元数据分析完成 - 尺寸: {}x{}, GIF动画: {}", 
                         image.getWidth(), image.getHeight(), imageMetadata.getIsAnimated());
            }
        } catch (Exception e) {
            log.warn("图片元数据分析失败: {}", e.getMessage());
        }
    }

    /**
     * 分析GIF动画信息
     */
    private void analyzeGifAnimation(MaterialMetadata.ImageMetadata.ImageMetadataBuilder imageBuilder, File file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                
                // 🔍 分析GIF帧信息
                int frameCount = reader.getNumImages(true);
                boolean isAnimated = frameCount > 1;
                
                long totalDuration = 0; // 毫秒
                int loopCount = 0;
                
                if (isAnimated) {
                    // 📊 分析每帧的延迟时间
                    List<Integer> frameDelays = new ArrayList<>();
                    
                    for (int i = 0; i < frameCount; i++) {
                        try {
                            IIOMetadata imageMetadata = reader.getImageMetadata(i);
                            if (imageMetadata != null) {
                                // 解析GIF帧延迟（GIF使用1/100秒为单位）
                                String[] metadataNames = imageMetadata.getMetadataFormatNames();
                                for (String format : metadataNames) {
                                    // 简化实现：使用默认帧延迟
                                    int frameDelay = 100; // 默认100毫秒
                                    frameDelays.add(frameDelay);
                                    totalDuration += frameDelay;
                                }
                            }
                        } catch (Exception e) {
                            log.debug("解析GIF帧{}延迟失败: {}", i, e.getMessage());
                            // 使用默认延迟
                            int defaultDelay = 100;
                            frameDelays.add(defaultDelay);
                            totalDuration += defaultDelay;
                        }
                    }
                    
                    // 计算平均帧延迟
                    double averageDelay = frameDelays.isEmpty() ? 100.0 : 
                                         frameDelays.stream().mapToInt(Integer::intValue).average().orElse(100.0);
                    
                    // 🔧 设置GIF动画属性
                    imageBuilder
                        .isAnimated(true)
                        .frameCount(frameCount)
                        .animationDuration(totalDuration)
                        .loopCount(loopCount) // GIF默认无限循环
                        .averageFrameDelay(averageDelay);
                    
                    log.debug("GIF动画分析完成 - 帧数: {}, 总时长: {}ms, 平均延迟: {}ms", 
                             frameCount, totalDuration, averageDelay);
                } else {
                    // 静态GIF
                    imageBuilder
                        .isAnimated(false)
                        .frameCount(1)
                        .animationDuration(null)
                        .loopCount(null)
                        .averageFrameDelay(null);
                    
                    log.debug("静态GIF图片分析完成");
                }
                
                reader.dispose();
            } else {
                log.warn("未找到GIF格式的ImageReader");
                // 设置默认值
                imageBuilder
                    .isAnimated(false)
                    .frameCount(1)
                    .animationDuration(null)
                    .loopCount(null)
                    .averageFrameDelay(null);
            }
        } catch (Exception e) {
            log.error("GIF动画分析失败: {}", e.getMessage(), e);
            // 设置默认值
            imageBuilder
                .isAnimated(false)
                .frameCount(null)
                .animationDuration(null)
                .loopCount(null)
                .averageFrameDelay(null);
        }
    }

    /**
     * 检测图片是否实际使用了透明度
     */
    private boolean detectActualTransparency(BufferedImage image, String mimeType) {
        try {
            // 🚀 首先检查颜色模型是否支持透明度
            if (!image.getColorModel().hasAlpha()) {
                return false;
            }
            
            int width = image.getWidth();
            int height = image.getHeight();
            
            // 🔧 根据图片格式使用不同的检测策略
            if ("image/png".equalsIgnoreCase(mimeType)) {
                return detectPngTransparency(image, width, height);
            } else if ("image/gif".equalsIgnoreCase(mimeType)) {
                return detectGifTransparency(image, width, height);
            } else {
                // 对于其他格式，使用通用检测方法
                return detectGeneralTransparency(image, width, height);
            }
            
        } catch (Exception e) {
            log.debug("透明度检测失败，使用颜色模型判断: {}", e.getMessage());
            return image.getColorModel().hasAlpha();
        }
    }
    
    /**
     * 检测PNG图片的透明度
     */
    private boolean detectPngTransparency(BufferedImage image, int width, int height) {
        // 🎯 PNG透明度检测：采样检测法，避免处理大图片时的性能问题
        int sampleSize = Math.min(100, Math.max(width, height)); // 最多采样100个像素
        int stepX = Math.max(1, width / sampleSize);
        int stepY = Math.max(1, height / sampleSize);
        
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                
                // 如果发现任何非完全不透明的像素（alpha < 255）
                if (alpha < 255) {
                    log.debug("PNG透明度检测：发现透明像素 at ({}, {}), alpha={}", x, y, alpha);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检测GIF图片的透明度
     */
    private boolean detectGifTransparency(BufferedImage image, int width, int height) {
        // 🎯 GIF透明度检测：GIF使用索引色彩模式，需要检查调色板
        try {
            // GIF图片通常使用IndexColorModel
            if (image.getColorModel() instanceof java.awt.image.IndexColorModel) {
                java.awt.image.IndexColorModel icm = (java.awt.image.IndexColorModel) image.getColorModel();
                
                // 检查是否有透明颜色索引
                int transparentPixel = icm.getTransparentPixel();
                if (transparentPixel != -1) {
                    log.debug("GIF透明度检测：发现透明色索引 {}", transparentPixel);
                    return true;
                }
            }
            
            // 备用方法：像素采样检测
            return detectGeneralTransparency(image, width, height);
            
        } catch (Exception e) {
            log.debug("GIF透明度检测异常: {}", e.getMessage());
            return detectGeneralTransparency(image, width, height);
        }
    }
    
    /**
     * 通用透明度检测方法
     */
    private boolean detectGeneralTransparency(BufferedImage image, int width, int height) {
        // 🚀 性能优化：对于大图片，只检测边缘和中心区域
        if (width > 1000 || height > 1000) {
            return detectTransparencyOptimized(image, width, height);
        }
        
        // 🔍 小图片：完整检测（最多检测前1000个像素）
        int maxPixels = Math.min(1000, width * height);
        int stepSize = Math.max(1, (width * height) / maxPixels);
        
        int checkedPixels = 0;
        for (int y = 0; y < height && checkedPixels < maxPixels; y++) {
            for (int x = 0; x < width && checkedPixels < maxPixels; x += stepSize) {
                int rgb = image.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                
                if (alpha < 255) {
                    log.debug("通用透明度检测：发现透明像素 at ({}, {}), alpha={}", x, y, alpha);
                    return true;
                }
                checkedPixels++;
            }
        }
        
        return false;
    }
    
    /**
     * 大图片的优化透明度检测
     */
    private boolean detectTransparencyOptimized(BufferedImage image, int width, int height) {
        // 🎯 检测策略：边缘检测 + 中心区域采样
        int edgeSize = Math.min(50, Math.min(width, height) / 4); // 边缘检测范围
        
        // 检测四个边缘
        for (int i = 0; i < edgeSize; i++) {
            // 上边缘
            for (int x = 0; x < width; x += 10) {
                if (isTransparentPixel(image.getRGB(x, i))) return true;
            }
            // 下边缘
            for (int x = 0; x < width; x += 10) {
                if (isTransparentPixel(image.getRGB(x, height - 1 - i))) return true;
            }
            // 左边缘
            for (int y = 0; y < height; y += 10) {
                if (isTransparentPixel(image.getRGB(i, y))) return true;
            }
            // 右边缘
            for (int y = 0; y < height; y += 10) {
                if (isTransparentPixel(image.getRGB(width - 1 - i, y))) return true;
            }
        }
        
        // 中心区域采样检测
        int centerX = width / 2;
        int centerY = height / 2;
        int sampleRadius = Math.min(100, Math.min(width, height) / 8);
        
        for (int dy = -sampleRadius; dy <= sampleRadius; dy += 10) {
            for (int dx = -sampleRadius; dx <= sampleRadius; dx += 10) {
                int x = centerX + dx;
                int y = centerY + dy;
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    if (isTransparentPixel(image.getRGB(x, y))) {
                        log.debug("大图透明度检测：中心区域发现透明像素 at ({}, {})", x, y);
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查单个像素是否透明
     */
    private boolean isTransparentPixel(int rgb) {
        int alpha = (rgb >> 24) & 0xFF;
        return alpha < 255; // 任何非完全不透明的像素都视为透明
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
     * 提取EXIF信息
     */
    private MaterialMetadata.ExifInfo extractExifInfo(File file) {
        try {
            // 🔍 使用Apache Tika提取EXIF信息
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            
            try (FileInputStream inputStream = new FileInputStream(file)) {
                parser.parse(inputStream, handler, metadata);
                
                return buildExifInfoFromTika(metadata);
            }
            
        } catch (Exception e) {
            log.debug("EXIF信息提取失败，尝试使用ImageIO: {}", e.getMessage());
            // 备用方法：使用ImageIO
            return extractExifWithImageIO(file);
        }
    }
    
    /**
     * 从Tika元数据构建EXIF信息
     */
    private MaterialMetadata.ExifInfo buildExifInfoFromTika(Metadata metadata) {
        try {
            MaterialMetadata.ExifInfo.ExifInfoBuilder builder = MaterialMetadata.ExifInfo.builder();
            
            // 🎯 关键的Orientation信息
            String orientation = metadata.get("tiff:Orientation");
            if (StringUtils.hasText(orientation)) {
                builder.orientation(normalizeOrientation(orientation));
            }
            
            // 📷 相机信息
            String cameraMake = metadata.get("tiff:Make");
            if (StringUtils.hasText(cameraMake)) {
                builder.cameraMake(cameraMake.trim());
            }
            
            String cameraModel = metadata.get("tiff:Model");
            if (StringUtils.hasText(cameraModel)) {
                builder.cameraModel(cameraModel.trim());
            }
            
            // 📅 拍摄时间
            String dateTime = metadata.get("exif:DateTimeOriginal");
            if (StringUtils.hasText(dateTime)) {
                try {
                    // 尝试解析EXIF日期格式：YYYY:MM:DD HH:MM:SS
                    LocalDateTime dateTaken = parseExifDateTime(dateTime);
                    builder.dateTaken(dateTaken);
                } catch (Exception e) {
                    log.debug("EXIF日期解析失败: {}", dateTime);
                }
            }
            
            // 🔧 镜头和拍摄参数
            String lensModel = metadata.get("exif:LensModel");
            if (StringUtils.hasText(lensModel)) {
                builder.lensModel(lensModel.trim());
            }
            
            String focalLength = metadata.get("exif:FocalLength");
            if (StringUtils.hasText(focalLength)) {
                try {
                    builder.focalLength(parseExifDouble(focalLength));
                } catch (Exception e) {
                    log.debug("焦距解析失败: {}", focalLength);
                }
            }
            
            String aperture = metadata.get("exif:FNumber");
            if (StringUtils.hasText(aperture)) {
                builder.aperture("f/" + aperture);
            }
            
            String shutterSpeed = metadata.get("exif:ExposureTime");
            if (StringUtils.hasText(shutterSpeed)) {
                builder.shutterSpeed(shutterSpeed);
            }
            
            String iso = metadata.get("exif:ISOSpeedRatings");
            if (StringUtils.hasText(iso)) {
                try {
                    builder.iso(Integer.parseInt(iso));
                } catch (Exception e) {
                    log.debug("ISO解析失败: {}", iso);
                }
            }
            
            // 📍 GPS信息
            String gpsLat = metadata.get("geo:lat");
            String gpsLon = metadata.get("geo:long");
            if (StringUtils.hasText(gpsLat) && StringUtils.hasText(gpsLon)) {
                try {
                    builder.gpsLatitude(Double.parseDouble(gpsLat));
                    builder.gpsLongitude(Double.parseDouble(gpsLon));
                } catch (Exception e) {
                    log.debug("GPS坐标解析失败: lat={}, lon={}", gpsLat, gpsLon);
                }
            }
            
            log.debug("EXIF信息提取完成 - Orientation: {}, 相机: {} {}", 
                     orientation, cameraMake, cameraModel);
            
            return builder.build();
            
        } catch (Exception e) {
            log.warn("构建EXIF信息失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 备用EXIF提取方法
     */
    private MaterialMetadata.ExifInfo extractExifWithImageIO(File file) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                
                IIOMetadata metadata = reader.getImageMetadata(0);
                if (metadata != null) {
                    // 简化的EXIF处理，主要提取orientation
                    MaterialMetadata.ExifInfo.ExifInfoBuilder builder = MaterialMetadata.ExifInfo.builder();
                    
                    // 尝试从元数据中提取orientation
                    String[] formatNames = metadata.getMetadataFormatNames();
                    for (String formatName : formatNames) {
                        try {
                            org.w3c.dom.Node tree = metadata.getAsTree(formatName);
                            String orientation = extractOrientationFromTree(tree);
                            if (StringUtils.hasText(orientation)) {
                                builder.orientation(normalizeOrientation(orientation));
                                break;
                            }
                        } catch (Exception e) {
                            log.debug("从{}格式提取orientation失败: {}", formatName, e.getMessage());
                        }
                    }
                    
                    return builder.build();
                }
                
                reader.dispose();
            }
        } catch (Exception e) {
            log.debug("ImageIO EXIF提取失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 标准化orientation值
     */
    private String normalizeOrientation(String orientation) {
        if (!StringUtils.hasText(orientation)) {
            return "1"; // 默认正常方向
        }
        
        // 清理orientation值，只保留数字
        String cleaned = orientation.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) {
            return "1";
        }
        
        try {
            int value = Integer.parseInt(cleaned);
            // EXIF Orientation取值范围是1-8
            if (value >= 1 && value <= 8) {
                return String.valueOf(value);
            }
        } catch (NumberFormatException e) {
            log.debug("Orientation值格式异常: {}", orientation);
        }
        
        return "1"; // 默认值
    }
    
    /**
     * 解析EXIF日期时间
     */
    private LocalDateTime parseExifDateTime(String dateTime) {
        // EXIF日期格式：YYYY:MM:DD HH:MM:SS
        if (!StringUtils.hasText(dateTime)) {
            return null;
        }
        
        try {
            // 替换前两个冒号为标准日期分隔符 (YYYY:MM:DD HH:MM:SS -> YYYY-MM-DD HH:MM:SS)
            String normalized = dateTime;
            if (dateTime.length() >= 10) {
                normalized = dateTime.substring(0, 4) + "-" + 
                           dateTime.substring(5, 7) + "-" + 
                           dateTime.substring(8);
            }
            return LocalDateTime.parse(normalized.replace(" ", "T"));
        } catch (Exception e) {
            log.debug("EXIF日期时间解析失败: {}", dateTime);
            return null;
        }
    }
    
    /**
     * 解析EXIF浮点数值
     */
    private Double parseExifDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        
        try {
            // 处理分数格式，如"50/1"
            if (value.contains("/")) {
                String[] parts = value.split("/");
                if (parts.length == 2) {
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    return denominator != 0 ? numerator / denominator : null;
                }
            }
            
            return Double.parseDouble(value);
        } catch (Exception e) {
            log.debug("EXIF数值解析失败: {}", value);
            return null;
        }
    }
    
    /**
     * 从DOM树中提取orientation信息
     */
    private String extractOrientationFromTree(org.w3c.dom.Node node) {
        // 简化实现，实际应该递归遍历DOM树查找orientation节点
        // 这里只是示例代码
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
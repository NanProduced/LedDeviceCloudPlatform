package org.nan.cloud.file.infrastructure.progress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.apache.tika.Tika;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.nan.cloud.file.application.domain.FileInfo;
import org.nan.cloud.file.application.service.StorageService;
import org.nan.cloud.file.application.service.ThumbnailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// JavaCV相关导入 - 用于视频处理
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.ffmpeg.global.avutil;

/**
 * 缩略图生成服务实现
 * 
 * 基于Thumbnailator库实现高质量图片缩略图生成。
 * 支持多种图片格式和尺寸配置，提供异步批量处理能力。
 * 
 * 支持的图片格式：JPG, JPEG, PNG, GIF, BMP, WEBP
 * 配置化的缩略图尺寸和质量设置
 * 
 * 特性：
 * 1. 多尺寸缩略图生成（小、中、大图）
 * 2. 高质量图片压缩和优化
 * 3. 异步批量处理支持
 * 4. 智能错误处理和降级策略
 * 5. 缩略图去重和版本管理
 * 6. 定期清理过期临时文件
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailServiceImpl implements ThumbnailService {

    private final StorageService storageService;
    
    private final FileStorageProperties storageProperties;
    
    /**
     * 支持的图片格式
     */
    private static final Set<String> SUPPORTED_IMAGE_FORMATS = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", 
            "image/bmp", "image/webp", "image/tiff"
    );
    
    /**
     * 支持的图片文件扩展名
     */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff", "tif"
    );
    
    /**
     * 支持的视频格式
     */
    private static final Set<String> SUPPORTED_VIDEO_FORMATS = Set.of(
            "video/mp4", "video/avi", "video/mov", "video/mkv", 
            "video/wmv", "video/flv", "video/webm", "video/m4v"
    );
    
    /**
     * 支持的视频文件扩展名
     */
    private static final Set<String> SUPPORTED_VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v"
    );
    
    /**
     * 缩略图缓存 - 避免重复生成
     */
    private final Map<String, List<ThumbnailInfo>> thumbnailCache = new ConcurrentHashMap<>();
    
    /**
     * Tika实例用于文件类型检测
     */
    private final Tika tika = new Tika();
    
    // 缩略图配置 - 统一通过 FileStorageProperties 获取
    private boolean thumbnailEnabled;
    private List<String> thumbnailSizes;
    private double thumbnailQuality;
    private String thumbnailFormat;
    private boolean videoThumbnailEnabled;
    private double videoFrameRate;
    private boolean gpuAcceleration;
    
    /**
     * 缩略图存储基础路径
     */
    private String thumbnailBasePath;
    
    /**
     * 解析后的缩略图尺寸配置
     */
    private List<ThumbnailSizeConfig> sizeConfigs;

    @PostConstruct
    public void initialize() {
        // 从统一配置中获取缩略图配置
        FileStorageProperties.Thumbnail thumbnailConfig = storageProperties.getThumbnail();
        this.thumbnailEnabled = thumbnailConfig.isEnabled();
        this.thumbnailSizes = thumbnailConfig.getSizes();
        this.thumbnailQuality = thumbnailConfig.getQuality();
        this.thumbnailFormat = thumbnailConfig.getFormat();
        this.videoThumbnailEnabled = thumbnailConfig.getVideo().isEnabled();
        this.videoFrameRate = thumbnailConfig.getVideo().getFrameRate();
        this.gpuAcceleration = thumbnailConfig.getVideo().isGpuAcceleration();
        
        // 初始化缩略图存储路径 - 使用统一配置
        this.thumbnailBasePath = storageProperties.getStorage().getLocal().getThumbnailBasePath();
        createThumbnailDirectories();
        
        // 解析缩略图尺寸配置
        parseThumbnailSizes();
        
        // 初始化JavaCV日志级别（减少日志输出）
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        
        log.info("ThumbnailService initialized - enabled: {}, videoEnabled: {}, sizes: {}, quality: {}, format: {}, gpuAccel: {}", 
                thumbnailEnabled, videoThumbnailEnabled, thumbnailSizes, thumbnailQuality, thumbnailFormat, gpuAcceleration);
    }

    @Override
    public ThumbnailResult generateImageThumbnail(FileInfo fileInfo) {
        if (!thumbnailEnabled) {
            return ThumbnailResult.failure("缩略图生成功能已禁用");
        }
        
        if (!isImageFile(fileInfo)) {
            return ThumbnailResult.failure("不支持的文件类型: " + fileInfo.getMimeType());
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始生成图片缩略图 - 文件ID: {}, 原始文件: {}", 
                    fileInfo.getFileId(), fileInfo.getOriginalFilename());
            
            // 检查缓存
            List<ThumbnailInfo> cachedThumbnails = thumbnailCache.get(fileInfo.getFileId());
            if (cachedThumbnails != null && !cachedThumbnails.isEmpty()) {
                log.debug("使用缓存的缩略图 - 文件ID: {}", fileInfo.getFileId());
                return ThumbnailResult.success(cachedThumbnails);
            }
            
            // 获取原始文件路径
            String originalFilePath = getAbsoluteFilePath(fileInfo.getStoragePath());
            File originalFile = new File(originalFilePath);
            
            if (!originalFile.exists()) {
                return ThumbnailResult.failure("原始文件不存在: " + originalFilePath);
            }
            
            // 生成所有配置的尺寸
            List<ThumbnailInfo> thumbnails = new ArrayList<>();
            
            for (ThumbnailSizeConfig sizeConfig : sizeConfigs) {
                try {
                    ThumbnailInfo thumbnail = generateSingleThumbnail(
                        fileInfo, originalFile, sizeConfig.width, sizeConfig.height);

                    thumbnails.add(thumbnail);
                    log.debug("生成缩略图成功 - {}x{}, 文件: {}",
                            sizeConfig.width, sizeConfig.height, thumbnail.getThumbnailPath());
                } catch (Exception e) {
                    log.warn("图片缩略图 - 生成{}x{}缩略图失败 - 文件ID: {}, 错误: {}",
                            sizeConfig.width, sizeConfig.height, fileInfo.getFileId(), e.getMessage());
                }
            }
            
            if (thumbnails.isEmpty()) {
                return ThumbnailResult.failure("所有尺寸的缩略图生成都失败");
            }
            
            // 缓存结果
            thumbnailCache.put(fileInfo.getFileId(), thumbnails);
            
            long processingTime = System.currentTimeMillis() - startTime;
            ThumbnailResult result = ThumbnailResult.success(thumbnails);
            result.setProcessingTime(processingTime);
            
            log.info("图片缩略图生成完成 - 文件ID: {}, 生成数量: {}, 耗时: {}ms", 
                    fileInfo.getFileId(), thumbnails.size(), processingTime);
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("生成图片缩略图失败 - 文件ID: {}, 耗时: {}ms", 
                    fileInfo.getFileId(), processingTime, e);
            return ThumbnailResult.failure("缩略图生成异常: " + e.getMessage());
        }
    }

    @Override
    public ThumbnailResult generateVideoThumbnail(FileInfo fileInfo, double timeOffset) {
        if (!thumbnailEnabled || !videoThumbnailEnabled) {
            return ThumbnailResult.failure("视频缩略图生成功能已禁用");
        }
        
        if (!isVideoFile(fileInfo)) {
            return ThumbnailResult.failure("不支持的视频文件类型: " + fileInfo.getMimeType());
        }
        
        long startTime = System.currentTimeMillis();
        FFmpegFrameGrabber frameGrabber = null;
        
        try {
            log.debug("开始生成视频缩略图 - 文件ID: {}, 时间偏移: {}s", fileInfo.getFileId(), timeOffset);
            
            // 检查缓存
            List<ThumbnailInfo> cachedThumbnails = thumbnailCache.get(fileInfo.getFileId());
            if (cachedThumbnails != null && !cachedThumbnails.isEmpty()) {
                log.debug("使用缓存的视频缩略图 - 文件ID: {}", fileInfo.getFileId());
                return ThumbnailResult.success(cachedThumbnails);
            }
            
            // 获取原始视频文件路径
            String originalFilePath = getAbsoluteFilePath(fileInfo.getStoragePath());
            File originalFile = new File(originalFilePath);
            
            if (!originalFile.exists()) {
                return ThumbnailResult.failure("原始视频文件不存在: " + originalFilePath);
            }
            
            // 初始化FFmpeg帧抓取器
            frameGrabber = new FFmpegFrameGrabber(originalFilePath);
            
            // 配置GPU加速（如果启用）
            if (gpuAcceleration) {
                // 尝试使用NVIDIA NVDEC硬件解码
                frameGrabber.setVideoOption("hwaccel", "nvdec");
                frameGrabber.setVideoOption("hwaccel_device", "0");
                log.debug("启用GPU硬件加速 - NVIDIA NVDEC");
            }
            
            frameGrabber.start();
            
            // 获取视频信息
            double duration = frameGrabber.getLengthInTime() / 1000000.0; // 转换为秒
            int totalFrames = frameGrabber.getLengthInFrames();
            double fps = frameGrabber.getFrameRate();
            
            log.debug("视频信息 - 时长: {}s, 总帧数: {}, 帧率: {}", duration, totalFrames, fps);
            
            // 调整时间偏移量（确保在视频范围内）
            double actualTimeOffset = Math.max(0, Math.min(timeOffset, duration - 1));
            if (actualTimeOffset != timeOffset) {
                log.debug("调整时间偏移量 - 原始: {}s, 实际: {}s", timeOffset, actualTimeOffset);
            }
            
            // 跳转到指定时间点
            long timestampMicros = (long) (actualTimeOffset * 1000000);
            frameGrabber.setTimestamp(timestampMicros);
            
            // 抓取帧
            Frame frame = frameGrabber.grabImage();
            if (frame == null) {
                return ThumbnailResult.failure("无法从视频中抓取帧");
            }
            
            // 转换为BufferedImage
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage image = converter.convert(frame);
            
            if (image == null) {
                return ThumbnailResult.failure("帧转换为图片失败");
            }
            
            // 生成所有配置的尺寸
            List<ThumbnailInfo> thumbnails = new ArrayList<>();
            
            for (ThumbnailSizeConfig sizeConfig : sizeConfigs) {
                try {
                    ThumbnailInfo thumbnail = generateVideoThumbnailFromImage(
                        fileInfo, image, sizeConfig.width, sizeConfig.height, actualTimeOffset);

                    thumbnails.add(thumbnail);
                    log.debug("生成视频缩略图成功 - {}x{}, 文件: {}",
                            sizeConfig.width, sizeConfig.height, thumbnail.getThumbnailPath());
                } catch (Exception e) {
                    log.warn("视频缩略图 - 生成{}x{}视频缩略图失败 - 文件ID: {}, 错误: {}",
                            sizeConfig.width, sizeConfig.height, fileInfo.getFileId(), e.getMessage());
                }
            }
            
            if (thumbnails.isEmpty()) {
                return ThumbnailResult.failure("所有尺寸的视频缩略图生成都失败");
            }
            
            // 缓存结果
            thumbnailCache.put(fileInfo.getFileId(), thumbnails);
            
            long processingTime = System.currentTimeMillis() - startTime;
            ThumbnailResult result = ThumbnailResult.success(thumbnails);
            result.setProcessingTime(processingTime);
            
            log.info("视频缩略图生成完成 - 文件ID: {}, 生成数量: {}, 时间偏移: {}s, 耗时: {}ms", 
                    fileInfo.getFileId(), thumbnails.size(), actualTimeOffset, processingTime);
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("生成视频缩略图失败 - 文件ID: {}, 耗时: {}ms", 
                    fileInfo.getFileId(), processingTime, e);
            return ThumbnailResult.failure("视频缩略图生成异常: " + e.getMessage());
        } finally {
            // 确保释放资源
            if (frameGrabber != null) {
                try {
                    frameGrabber.stop();
                    frameGrabber.release();
                } catch (Exception e) {
                    log.warn("释放FFmpeg资源失败", e);
                }
            }
        }
    }

    @Override
    @Async
    public void generateThumbnailAsync(FileInfo fileInfo) {
        generateThumbnailAsync(fileInfo, null);
    }

    @Override
    @Async
    public void generateThumbnailAsync(FileInfo fileInfo, ThumbnailCallback callback) {
        log.debug("异步生成缩略图开始 - 文件ID: {}", fileInfo.getFileId());
        
        try {
            ThumbnailResult result;
            
            // 根据文件类型选择相应的缩略图生成方法
            if (isVideoFile(fileInfo)) {
                // 视频文件：在第5秒处生成缩略图
                result = generateVideoThumbnail(fileInfo, 5.0);
            } else if (isImageFile(fileInfo)) {
                // 图片文件：直接生成缩略图
                result = generateImageThumbnail(fileInfo);
            } else {
                log.warn("不支持的文件类型 - 文件ID: {}, MIME类型: {}", 
                        fileInfo.getFileId(), fileInfo.getMimeType());
                result = ThumbnailResult.failure("不支持的文件类型: " + fileInfo.getMimeType());
            }
            
            if (result.isSuccess()) {
                log.info("异步缩略图生成成功 - 文件ID: {}, 数量: {}", 
                        fileInfo.getFileId(), result.getThumbnails().size());
            } else {
                log.warn("异步缩略图生成失败 - 文件ID: {}, 错误: {}", 
                        fileInfo.getFileId(), result.getErrorMessage());
            }
            
            // 调用回调函数
            if (callback != null) {
                callback.onThumbnailGenerated(fileInfo.getFileId(), result);
            }
            
        } catch (Exception e) {
            log.error("异步缩略图生成异常 - 文件ID: {}", fileInfo.getFileId(), e);
            ThumbnailResult errorResult = ThumbnailResult.failure("缩略图生成异常: " + e.getMessage());
            if (callback != null) {
                callback.onThumbnailGenerated(fileInfo.getFileId(), errorResult);
            }
        }
    }

    @Override
    public BatchThumbnailResult batchGenerateThumbnails(List<FileInfo> fileInfos) {
        if (!thumbnailEnabled) {
            BatchThumbnailResult result = new BatchThumbnailResult(fileInfos.size());
            result.setFailedCount(fileInfos.size());
            result.setFailedFileIds(fileInfos.stream()
                    .map(FileInfo::getFileId)
                    .collect(Collectors.toList()));
            return result;
        }
        
        log.info("开始批量生成缩略图 - 文件数量: {}", fileInfos.size());
        long startTime = System.currentTimeMillis();
        
        BatchThumbnailResult batchResult = new BatchThumbnailResult(fileInfos.size());
        
        // 并行处理提高效率
        List<CompletableFuture<Void>> futures = fileInfos.stream()
                .map(fileInfo -> CompletableFuture.runAsync(() -> {
                    try {
                        ThumbnailResult result = generateImageThumbnail(fileInfo);
                        if (result.isSuccess()) {
                            batchResult.addSuccess();
                        } else {
                            batchResult.addFailure(fileInfo.getFileId());
                        }
                    } catch (Exception e) {
                        log.error("批量生成缩略图失败 - 文件ID: {}", fileInfo.getFileId(), e);
                        batchResult.addFailure(fileInfo.getFileId());
                    }
                }))
                .toList();
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        long processingTime = System.currentTimeMillis() - startTime;
        batchResult.setTotalProcessingTime(processingTime);
        
        log.info("批量缩略图生成完成 - 总数: {}, 成功: {}, 失败: {}, 耗时: {}ms", 
                fileInfos.size(), batchResult.getSuccessCount(), 
                batchResult.getFailedCount(), processingTime);
        
        return batchResult;
    }

    @Override
    public ThumbnailResult generateCustomSizeThumbnail(FileInfo fileInfo, int width, int height) {
        if (!thumbnailEnabled) {
            return ThumbnailResult.failure("缩略图生成功能已禁用");
        }
        
        if (!isImageFile(fileInfo)) {
            return ThumbnailResult.failure("不支持的文件类型: " + fileInfo.getMimeType());
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("生成自定义尺寸缩略图 - 文件ID: {}, 尺寸: {}x{}", 
                    fileInfo.getFileId(), width, height);
            
            String originalFilePath = getAbsoluteFilePath(fileInfo.getStoragePath());
            File originalFile = new File(originalFilePath);
            
            if (!originalFile.exists()) {
                return ThumbnailResult.failure("原始文件不存在: " + originalFilePath);
            }
            
            ThumbnailInfo thumbnail = generateSingleThumbnail(fileInfo, originalFile, width, height);

            long processingTime = System.currentTimeMillis() - startTime;
            ThumbnailResult result = ThumbnailResult.success(List.of(thumbnail));
            result.setProcessingTime(processingTime);
            
            return result;
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("生成自定义尺寸缩略图失败 - 文件ID: {}, 尺寸: {}x{}, 耗时: {}ms", 
                    fileInfo.getFileId(), width, height, processingTime, e);
            return ThumbnailResult.failure("自定义缩略图生成异常: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteThumbnails(String fileId) {
        try {
            log.debug("删除缩略图 - 文件ID: {}", fileId);
            
            // 从缓存中移除
            List<ThumbnailInfo> thumbnails = thumbnailCache.remove(fileId);
            
            if (thumbnails == null || thumbnails.isEmpty()) {
                log.debug("没有找到缓存的缩略图 - 文件ID: {}", fileId);
                return true;
            }
            
            // 删除物理文件
            int deletedCount = 0;
            for (ThumbnailInfo thumbnail : thumbnails) {
                try {
                    String thumbnailPath = thumbnail.getThumbnailPath();
                    if (StringUtils.hasText(thumbnailPath)) {
                        Path path = Paths.get(getAbsoluteFilePath(thumbnailPath));
                        if (Files.exists(path)) {
                            Files.delete(path);
                            deletedCount++;
                            log.debug("删除缩略图文件: {}", thumbnailPath);
                        }
                    }
                } catch (Exception e) {
                    log.warn("删除缩略图文件失败: {}", thumbnail.getThumbnailPath(), e);
                }
            }
            
            log.info("缩略图删除完成 - 文件ID: {}, 删除数量: {}/{}", 
                    fileId, deletedCount, thumbnails.size());
            return true;
            
        } catch (Exception e) {
            log.error("删除缩略图失败 - 文件ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    public List<ThumbnailInfo> getThumbnails(String fileId) {
        List<ThumbnailInfo> thumbnails = thumbnailCache.get(fileId);
        return thumbnails != null ? new ArrayList<>(thumbnails) : new ArrayList<>();
    }

    @Override
    public boolean hasThumbnails(String fileId) {
        List<ThumbnailInfo> thumbnails = thumbnailCache.get(fileId);
        return thumbnails != null && !thumbnails.isEmpty();
    }

    @Override
    public ThumbnailResult regenerateThumbnail(FileInfo fileInfo) {
        log.info("重新生成缩略图 - 文件ID: {}", fileInfo.getFileId());
        
        // 先删除现有缩略图
        deleteThumbnails(fileInfo.getFileId());
        
        // 重新生成
        return generateImageThumbnail(fileInfo);
    }

    @Override
    public String getThumbnailUrl(String thumbnailPath) {
        if (!StringUtils.hasText(thumbnailPath)) {
            return null;
        }
        
        // 通过StorageService生成访问URL
        return storageService.generateAccessUrl(thumbnailPath);
    }

    /**
     * 获取主缩略图路径（选择300x300作为主缩略图）
     * 
     * @param fileId 文件ID
     * @return 主缩略图路径，如果没有则返回null
     */
    public String getPrimaryThumbnailPath(String fileId) {
        List<ThumbnailInfo> thumbnails = getThumbnails(fileId);
        if (thumbnails == null || thumbnails.isEmpty()) {
            return null;
        }
        
        // 优先选择300x300的缩略图作为主缩略图
        for (ThumbnailInfo thumbnail : thumbnails) {
            if (thumbnail.getWidth() == 300 && thumbnail.getHeight() == 300) {
                return thumbnail.getThumbnailPath();
            }
        }
        
        // 如果没有300x300的，选择第一个作为主缩略图
        return thumbnails.get(0).getThumbnailPath();
    }

    @Override
    public List<String> getSupportedFileTypes() {
        return new ArrayList<>(SUPPORTED_IMAGE_FORMATS);
    }

    @Override
    public int cleanupExpiredThumbnails(int expireHours) {
        log.info("开始清理过期缩略图 - 过期时间: {}小时", expireHours);
        
        AtomicInteger cleanedCount = new AtomicInteger();
        long expireTime = System.currentTimeMillis() - (expireHours * 60 * 60 * 1000L);
        
        try {
            Path thumbnailDir = Paths.get(thumbnailBasePath);
            if (!Files.exists(thumbnailDir)) {
                return 0;
            }
            
            // 遍历缩略图目录，删除过期文件
            Files.walk(thumbnailDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < expireTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            cleanedCount.getAndIncrement();
                            log.debug("删除过期缩略图: {}", path);
                        } catch (Exception e) {
                            log.warn("删除过期缩略图失败: {}", path, e);
                        }
                    });
            
            // 清理缓存中的过期条目
            thumbnailCache.entrySet().removeIf(entry -> {
                List<ThumbnailInfo> thumbnails = entry.getValue();
                return thumbnails.stream().anyMatch(thumb -> 
                        thumb.getCreateTime().isBefore(LocalDateTime.now().minusHours(expireHours)));
            });
            
        } catch (Exception e) {
            log.error("清理过期缩略图失败", e);
        }
        
        log.info("过期缩略图清理完成 - 清理数量: {}", cleanedCount);
        return cleanedCount.get();
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 生成单个缩略图
     */
    private ThumbnailInfo generateSingleThumbnail(FileInfo fileInfo, File originalFile, 
                                                 int width, int height) throws IOException {
        // 生成缩略图文件名
        String thumbnailFileName = generateThumbnailFileName(fileInfo, width, height);
        String thumbnailRelativePath = generateThumbnailRelativePath(fileInfo.getFileId(), thumbnailFileName);
        String thumbnailAbsolutePath = getAbsoluteFilePath(thumbnailRelativePath);
        
        // 确保目录存在
        File thumbnailFile = new File(thumbnailAbsolutePath);
        thumbnailFile.getParentFile().mkdirs();
        
        // 使用Thumbnailator生成缩略图
        BufferedImage thumbnail = Thumbnails.of(originalFile)
                .size(width, height)
                .keepAspectRatio(true)
                .crop(Positions.CENTER)
                .outputQuality(thumbnailQuality)
                .asBufferedImage();
        
        // 保存缩略图
        Thumbnails.of(thumbnail)
                .size(width, height)
                .outputFormat(thumbnailFormat)
                .outputQuality(thumbnailQuality)
                .toFile(thumbnailFile);
        
        // 创建缩略图信息
        ThumbnailInfo thumbnailInfo = new ThumbnailInfo(
                fileInfo.getFileId(), thumbnailRelativePath, width, height);
        
        thumbnailInfo.setThumbnailId(UUID.randomUUID().toString());
        thumbnailInfo.setFileSize(thumbnailFile.length());
        thumbnailInfo.setFormat(thumbnailFormat.toUpperCase());
        thumbnailInfo.setQuality(thumbnailQuality);
        
        return thumbnailInfo;
    }
    
    /**
     * 检查是否为图片文件
     */
    private boolean isImageFile(FileInfo fileInfo) {
        // 首先检查MIME类型
        if (StringUtils.hasText(fileInfo.getMimeType()) && 
            SUPPORTED_IMAGE_FORMATS.contains(fileInfo.getMimeType().toLowerCase())) {
            return true;
        }
        
        // 检查文件扩展名
        String filename = fileInfo.getOriginalFilename();
        if (StringUtils.hasText(filename)) {
            String extension = getFileExtension(filename).toLowerCase();
            return SUPPORTED_EXTENSIONS.contains(extension);
        }
        
        return false;
    }
    
    /**
     * 检查是否为视频文件
     */
    private boolean isVideoFile(FileInfo fileInfo) {
        // 首先检查MIME类型
        if (StringUtils.hasText(fileInfo.getMimeType()) && 
            SUPPORTED_VIDEO_FORMATS.contains(fileInfo.getMimeType().toLowerCase())) {
            return true;
        }
        
        // 检查文件扩展名
        String filename = fileInfo.getOriginalFilename();
        if (StringUtils.hasText(filename)) {
            String extension = getFileExtension(filename).toLowerCase();
            return SUPPORTED_VIDEO_EXTENSIONS.contains(extension);
        }
        
        return false;
    }
    
    /**
     * 从BufferedImage生成视频缩略图文件
     */
    private ThumbnailInfo generateVideoThumbnailFromImage(FileInfo fileInfo, BufferedImage image, 
                                                        int width, int height, double timeOffset) throws IOException {
        // 生成缩略图文件名 - 视频缩略图添加时间偏移标识
        String thumbnailFileName = generateVideoThumbnailFileName(fileInfo, width, height, timeOffset);
        String thumbnailRelativePath = generateThumbnailRelativePath(fileInfo.getFileId(), thumbnailFileName);
        String thumbnailAbsolutePath = getAbsoluteFilePath(thumbnailRelativePath);
        
        // 确保目录存在
        File thumbnailFile = new File(thumbnailAbsolutePath);
        thumbnailFile.getParentFile().mkdirs();
        
        // 使用Thumbnailator处理BufferedImage生成缩略图
        BufferedImage thumbnail = Thumbnails.of(image)
                .size(width, height)
                .keepAspectRatio(true)
                .crop(Positions.CENTER)
                .outputQuality(thumbnailQuality)
                .asBufferedImage();
        
        // 保存缩略图
        Thumbnails.of(thumbnail)
                .size(width, height)
                .outputFormat(thumbnailFormat)
                .outputQuality(thumbnailQuality)
                .toFile(thumbnailFile);
        
        // 创建缩略图信息
        ThumbnailInfo thumbnailInfo = new ThumbnailInfo(
                fileInfo.getFileId(), thumbnailRelativePath, width, height);
        
        thumbnailInfo.setThumbnailId(UUID.randomUUID().toString());
        thumbnailInfo.setFileSize(thumbnailFile.length());
        thumbnailInfo.setFormat(thumbnailFormat.toUpperCase());
        thumbnailInfo.setQuality(thumbnailQuality);
        
        return thumbnailInfo;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
    
    /**
     * 生成缩略图文件名
     */
    private String generateThumbnailFileName(FileInfo fileInfo, int width, int height) {
        String originalName = fileInfo.getOriginalFilename();
        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
        return String.format("%s_%dx%d.%s", baseName, width, height, thumbnailFormat);
    }
    
    /**
     * 生成视频缩略图文件名（包含时间偏移信息）
     */
    private String generateVideoThumbnailFileName(FileInfo fileInfo, int width, int height, double timeOffset) {
        String originalName = fileInfo.getOriginalFilename();
        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
        // 时间偏移格式化为整数秒，避免文件名过长
        int timeSeconds = (int) Math.round(timeOffset);
        return String.format("%s_%dx%d_t%ds.%s", baseName, width, height, timeSeconds, thumbnailFormat);
    }
    
    /**
     * 生成缩略图相对路径
     */
    private String generateThumbnailRelativePath(String fileId, String thumbnailFileName) {
        // 按日期分目录存储，避免单个目录文件过多
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("thumbnails/%s/%s/%s", dateStr, fileId, thumbnailFileName);
    }
    
    /**
     * 获取绝对文件路径
     */
    private String getAbsoluteFilePath(String relativePath) {
        return Paths.get(storageProperties.getStorage().getLocal().getBasePath(), relativePath).toString();
    }
    
    /**
     * 创建缩略图目录
     */
    private void createThumbnailDirectories() {
        try {
            Path thumbnailDir = Paths.get(thumbnailBasePath);
            if (!Files.exists(thumbnailDir)) {
                Files.createDirectories(thumbnailDir);
                log.info("创建缩略图目录: {}", thumbnailBasePath);
            }
        } catch (Exception e) {
            log.error("创建缩略图目录失败: {}", thumbnailBasePath, e);
        }
    }
    
    /**
     * 解析缩略图尺寸配置 
     */
    private void parseThumbnailSizes() {
        sizeConfigs = new ArrayList<>();
        
        if (thumbnailSizes != null && !thumbnailSizes.isEmpty()) {
            for (String sizeStr : thumbnailSizes) {
                try {
                    String[] parts = sizeStr.trim().split("x|X");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0].trim());
                        int height = Integer.parseInt(parts[1].trim());
                        sizeConfigs.add(new ThumbnailSizeConfig(width, height));
                    }
                } catch (Exception e) {
                    log.warn("解析缩略图尺寸配置失败: {}", sizeStr, e);
                }
            }
        }
        
        // 如果没有配置或解析失败，使用默认尺寸
        if (sizeConfigs.isEmpty()) {
            sizeConfigs.add(new ThumbnailSizeConfig(150, 150));
            sizeConfigs.add(new ThumbnailSizeConfig(300, 300));
            sizeConfigs.add(new ThumbnailSizeConfig(600, 600));
        }
        
        log.info("缩略图尺寸配置: {}", sizeConfigs);
    }
    
    /**
     * 缩略图尺寸配置类
     */
    private static class ThumbnailSizeConfig {
        private final int width;
        private final int height;
        
        public ThumbnailSizeConfig(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        @Override
        public String toString() {
            return width + "x" + height;
        }
    }
}
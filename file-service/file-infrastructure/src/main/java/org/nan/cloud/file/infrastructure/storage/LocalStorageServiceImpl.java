package org.nan.cloud.file.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.application.service.StorageService;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 本地文件系统存储服务实现
 * 
 * 职责：
 * 1. 本地文件系统的文件存储和管理
 * 2. 文件访问URL生成
 * 3. 存储空间统计和健康检查
 * 
 * 设计考虑：
 * - 按日期分目录存储，便于管理和备份
 * - 完善的错误处理和资源清理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageServiceImpl implements StorageService {

    // 文件存储配置属性
    private final FileStorageProperties storageProperties;

    @Override
    public String store(MultipartFile file, String fileId) {
        return store(file, null, fileId);
    }

    @Override
    public String store(MultipartFile file, FileUploadRequest request, String fileId) {
        log.info("开始存储文件 - 文件ID: {}, 文件名: {}, 大小: {}", 
                fileId, file.getOriginalFilename(), file.getSize());

        try {
            // 1. 验证文件大小
            validateFileSize(file.getSize());

            // 2. 确定存储路径
            String storagePath = generateStoragePath(fileId, file.getOriginalFilename());
            Path targetPath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);

            // 3. 创建目录
            Files.createDirectories(targetPath.getParent());

            // 4. 保存文件
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件存储成功 - 文件ID: {}, 存储路径: {}", fileId, storagePath);
            return storagePath;

        } catch (Exception e) {
            log.error("文件存储失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateAccessUrl(String storagePath) {
        // 本地存储返回相对路径，由网关或静态资源服务器处理
        return "/files/" + storagePath;
    }

    @Override
    public String generateTemporaryUrl(String storagePath, int expireMinutes) {
        // 本地存储暂不支持临时URL，返回普通访问URL
        return generateAccessUrl(storagePath);
    }

    @Override
    public boolean deleteFile(String storagePath) {
        try {
            Path filePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            boolean deleted = Files.deleteIfExists(filePath);
            
            if (deleted) {
                log.info("文件删除成功 - 路径: {}", storagePath);
            } else {
                log.warn("文件不存在或已删除 - 路径: {}", storagePath);
            }
            
            return deleted;
            
        } catch (Exception e) {
            log.error("文件删除失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int batchDeleteFiles(String[] storagePaths) {
        int deletedCount = 0;
        for (String path : storagePaths) {
            if (deleteFile(path)) {
                deletedCount++;
            }
        }
        log.info("批量删除完成 - 总数: {}, 成功: {}", storagePaths.length, deletedCount);
        return deletedCount;
    }

    @Override
    public boolean copyFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), sourcePath);
            Path target = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), targetPath);
            
            // 创建目标目录
            Files.createDirectories(target.getParent());
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件复制成功 - 源: {}, 目标: {}", sourcePath, targetPath);
            return true;
            
        } catch (Exception e) {
            log.error("文件复制失败 - 源: {}, 目标: {}, 错误: {}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean moveFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), sourcePath);
            Path target = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), targetPath);
            
            // 创建目标目录
            Files.createDirectories(target.getParent());
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件移动成功 - 源: {}, 目标: {}", sourcePath, targetPath);
            return true;
            
        } catch (Exception e) {
            log.error("文件移动失败 - 源: {}, 目标: {}, 错误: {}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            Path filePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.exists(filePath);
        } catch (Exception e) {
            log.error("检查文件存在性失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            Path filePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.size(filePath);
        } catch (Exception e) {
            log.error("获取文件大小失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public long getLastModified(String storagePath) {
        try {
            Path filePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.getLastModifiedTime(filePath).toMillis();
        } catch (Exception e) {
            log.error("获取文件最后修改时间失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public StorageStatistics getStorageStatistics() {
        try {
            Path basePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath());
            
            // 计算已用空间和文件数
            final long[] stats = {0, 0}; // [totalSize, fileCount]
            
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    stats[0] += attrs.size(); // 累计文件大小
                    stats[1]++; // 累计文件数
                    return FileVisitResult.CONTINUE;
                }
            });
            
            // 获取可用空间
            long usableSpace = Files.getFileStore(basePath).getUsableSpace();
            
            return new StorageStatistics(stats[1], stats[0], stats[0], usableSpace);
            
        } catch (Exception e) {
            log.error("获取存储统计信息失败", e);
            return new StorageStatistics(0, 0, 0, 0);
        }
    }

    @Override
    public StorageStrategy getStorageStrategy() {
        return StorageStrategy.LOCAL;
    }

    @Override
    public boolean healthCheck() {
        try {
            // 检查基础路径是否存在和可写
            Path basePath = Paths.get(storageProperties.getStorage().getLocal().getBasePath());
            
            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("基础存储目录不存在，已创建: {}", basePath);
            }
            
            if (!Files.isWritable(basePath)) {
                log.error("基础存储目录不可写: {}", basePath);
                return false;
            }
            
            // 尝试创建测试文件
            Path testFile = basePath.resolve("health-check.tmp");
            Files.writeString(testFile, "health-check", StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            Files.deleteIfExists(testFile);
            
            return true;
            
        } catch (Exception e) {
            log.error("存储服务健康检查失败", e);
            return false;
        }
    }

    @Override
    public String getAbsolutePath(String storagePath) {
        return Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath).toAbsolutePath().toString();
    }

    // 私有辅助方法

    private void validateFileSize(long fileSize) {
        long maxFileSize = storageProperties.getStorage().getLocal().getMaxFileSize();
        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("文件大小超出限制: %d bytes (最大: %d bytes)", fileSize, maxFileSize));
        }
    }

    private String generateStoragePath(String fileId, String originalFilename) {
        // 按日期分目录：YYYY/MM/DD/fileId.extension
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = getFileExtension(originalFilename);
        String filename = extension.isEmpty() ? fileId : fileId + "." + extension;
        
        return dateDir + "/" + filename;
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
}
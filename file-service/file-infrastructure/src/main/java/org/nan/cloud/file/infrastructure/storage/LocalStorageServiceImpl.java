package org.nan.cloud.file.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.dto.ChunkUploadInitRequest;
import org.nan.cloud.file.api.dto.ChunkUploadRequest;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.application.service.StorageService;
import org.nan.cloud.file.application.config.FileStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地文件系统存储服务实现
 * 
 * 职责：
 * 1. 本地文件系统的文件存储和管理
 * 2. 分片上传的支持和管理
 * 3. 文件访问URL生成
 * 4. 存储空间统计和健康检查
 * 
 * 设计考虑：
 * - 按日期分目录存储，便于管理和备份
 * - 支持大文件分片上传和合并
 * - 线程安全的分片上传管理
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
    
    // 分片上传信息缓存（生产环境应使用Redis）
    private final ConcurrentHashMap<String, ChunkUploadInfo> chunkUploadCache = new ConcurrentHashMap<>();
    
    // 文件操作锁（按uploadId分锁）
    private final ConcurrentHashMap<String, ReentrantLock> uploadLocks = new ConcurrentHashMap<>();

    @Override
    public String store(MultipartFile file, String fileId) {
        return store(file, null, fileId);
    }

    @Override
    public String store(MultipartFile file, FileUploadRequest request, String fileId) {
        log.info("开始存储文件 - 文件ID: {}, 原文件名: {}, 大小: {}", 
                fileId, file.getOriginalFilename(), file.getSize());
        
        try {
            // 1. 验证文件大小
            validateFileSize(file.getSize());
            
            // 2. 生成存储路径（包含组织隔离）
            Long organizationId = (request != null && request.getOid() != null) ? request.getOid() : 0L;
            String storagePath = generateStoragePath(fileId, file.getOriginalFilename(), organizationId);
            
            // 3. 确保目录存在
            ensureDirectoryExists(storagePath);
            
            // 4. 存储文件
            String rootPath = storageProperties.getStorage().getLocal().getBasePath();
            Path targetPath = Paths.get(rootPath, storagePath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 5. 验证存储结果
            if (!Files.exists(targetPath)) {
                throw new RuntimeException("文件存储失败，目标文件不存在");
            }
            
            log.info("文件存储成功 - 文件ID: {}, 存储路径: {}", fileId, storagePath);
            return storagePath;
            
        } catch (Exception e) {
            log.error("文件存储失败 - 文件ID: {}, 错误: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String initChunkUpload(String uploadId, ChunkUploadInitRequest request) {
        log.info("初始化分片上传 - 上传ID: {}, 文件名: {}, 总大小: {}, 分片大小: {}", 
                uploadId, request.getFilename(), request.getFileSize(), request.getChunkSize());
        
        try {
            // 1. 验证文件大小
            validateFileSize(request.getFileSize());
            
            // 2. 计算分片数量
            int totalChunks = (int) Math.ceil((double) request.getFileSize() / request.getChunkSize());
            
            // 3. 创建分片临时目录
            String chunkDir = createChunkTempDirectory(uploadId);
            
            // 4. 缓存上传信息
            ChunkUploadInfo uploadInfo = new ChunkUploadInfo(
                    uploadId, request.getFilename(), request.getFileSize(), 
                    request.getChunkSize(), totalChunks, chunkDir
            );
            chunkUploadCache.put(uploadId, uploadInfo);
            
            log.info("分片上传初始化成功 - 上传ID: {}, 总分片数: {}, 临时目录: {}", 
                    uploadId, totalChunks, chunkDir);
            
            return chunkDir;
            
        } catch (Exception e) {
            log.error("分片上传初始化失败 - 上传ID: {}, 错误: {}", uploadId, e.getMessage(), e);
            throw new RuntimeException("分片上传初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadChunk(MultipartFile chunk, ChunkUploadRequest request) {
        String uploadId = request.getUploadId();
        int chunkNumber = request.getChunkNumber();
        
        log.debug("上传文件分片 - 上传ID: {}, 分片号: {}", uploadId, chunkNumber);
        
        // 获取上传锁
        ReentrantLock lock = uploadLocks.computeIfAbsent(uploadId, k -> new ReentrantLock());
        lock.lock();
        
        try {
            // 1. 获取上传信息
            ChunkUploadInfo uploadInfo = chunkUploadCache.get(uploadId);
            if (uploadInfo == null) {
                throw new IllegalArgumentException("分片上传会话不存在: " + uploadId);
            }
            
            // 2. 验证分片号
            if (chunkNumber < 1 || chunkNumber > uploadInfo.getTotalChunks()) {
                throw new IllegalArgumentException("无效的分片号: " + chunkNumber);
            }
            
            // 3. 生成分片文件路径
            String chunkFileName = String.format("chunk_%d", chunkNumber);
            Path chunkPath = Paths.get(uploadInfo.getChunkDir(), chunkFileName);
            
            // 4. 存储分片
            try (InputStream inputStream = chunk.getInputStream()) {
                Files.copy(inputStream, chunkPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 5. 记录已上传分片
            uploadInfo.markChunkUploaded(chunkNumber);
            
            log.debug("文件分片上传成功 - 上传ID: {}, 分片号: {}, 已完成: {}/{}", 
                    uploadId, chunkNumber, uploadInfo.getUploadedChunks().size(), uploadInfo.getTotalChunks());
            
            return chunkPath.toString();
            
        } catch (Exception e) {
            log.error("文件分片上传失败 - 上传ID: {}, 分片号: {}, 错误: {}", 
                    uploadId, chunkNumber, e.getMessage(), e);
            throw new RuntimeException("分片上传失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String mergeChunks(String uploadId) {
        log.info("开始合并文件分片 - 上传ID: {}", uploadId);
        
        // 获取上传锁
        ReentrantLock lock = uploadLocks.computeIfAbsent(uploadId, k -> new ReentrantLock());
        lock.lock();
        
        try {
            // 1. 获取上传信息
            ChunkUploadInfo uploadInfo = chunkUploadCache.get(uploadId);
            if (uploadInfo == null) {
                throw new IllegalArgumentException("分片上传会话不存在: " + uploadId);
            }
            
            // 2. 验证所有分片已上传
            if (!isAllChunksUploaded(uploadId)) {
                throw new IllegalStateException("还有分片未上传完成");
            }
            
            // 3. 生成最终文件路径
            String finalPath = generateStoragePath(uploadId, uploadInfo.getFilename());
            ensureDirectoryExists(finalPath);
            Path targetPath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), finalPath);
            
            // 4. 合并分片文件
            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                
                for (int i = 1; i <= uploadInfo.getTotalChunks(); i++) {
                    String chunkFileName = String.format("chunk_%d", i);
                    Path chunkPath = Paths.get(uploadInfo.getChunkDir(), chunkFileName);
                    
                    try (FileInputStream fis = new FileInputStream(chunkPath.toFile());
                         BufferedInputStream bis = new BufferedInputStream(fis)) {
                        
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            bos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            
            // 5. 验证合并结果
            if (!Files.exists(targetPath)) {
                throw new RuntimeException("文件合并失败，目标文件不存在");
            }
            
            log.info("文件分片合并成功 - 上传ID: {}, 最终路径: {}, 文件大小: {}", 
                    uploadId, finalPath, Files.size(targetPath));
            
            return finalPath;
            
        } catch (Exception e) {
            log.error("文件分片合并失败 - 上传ID: {}, 错误: {}", uploadId, e.getMessage(), e);
            throw new RuntimeException("分片合并失败: " + e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cleanupChunks(String uploadId) {
        log.info("清理分片临时文件 - 上传ID: {}", uploadId);
        
        try {
            ChunkUploadInfo uploadInfo = chunkUploadCache.remove(uploadId);
            uploadLocks.remove(uploadId);
            
            if (uploadInfo != null && uploadInfo.getChunkDir() != null) {
                Path chunkDirPath = Paths.get(uploadInfo.getChunkDir());
                if (Files.exists(chunkDirPath)) {
                    // 删除分片目录及其内容
                    Files.walk(chunkDirPath)
                            .sorted((a, b) -> b.compareTo(a)) // 先删文件，后删目录
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException e) {
                                    log.warn("删除分片文件失败: {}, 错误: {}", path, e.getMessage());
                                }
                            });
                }
            }
            
            log.info("分片临时文件清理完成 - 上传ID: {}", uploadId);
            
        } catch (Exception e) {
            log.error("清理分片临时文件失败 - 上传ID: {}, 错误: {}", uploadId, e.getMessage(), e);
        }
    }

    @Override
    public boolean isAllChunksUploaded(String uploadId) {
        ChunkUploadInfo uploadInfo = chunkUploadCache.get(uploadId);
        if (uploadInfo == null) {
            return false;
        }
        return uploadInfo.getUploadedChunks().size() == uploadInfo.getTotalChunks();
    }

    @Override
    public String calculateMergedFileMD5(String filePath) {
        try {
            Path path = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), filePath);
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            try (InputStream is = Files.newInputStream(path);
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("计算文件MD5失败 - 路径: {}, 错误: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("计算文件MD5失败", e);
        }
    }

    @Override
    public ChunkUploadInitRequest getChunkUploadInfo(String uploadId) {
        ChunkUploadInfo uploadInfo = chunkUploadCache.get(uploadId);
        if (uploadInfo == null) {
            return null;
        }
        
        ChunkUploadInitRequest request = new ChunkUploadInitRequest();
        request.setFilename(uploadInfo.getFilename());
        request.setFileSize(uploadInfo.getFileSize());
        request.setChunkSize(uploadInfo.getChunkSize());
        return request;
    }

    @Override
    public String generateAccessUrl(String storagePath) {
        if (storagePath == null || storagePath.trim().isEmpty()) {
            return null;
        }
        return storageProperties.getStorage().getLocal().getUrlPrefix() + "/" + storagePath.replace("\\", "/");
    }

    @Override
    public String generateTemporaryUrl(String storagePath, int expireMinutes) {
        // 本地存储暂不支持临时URL，返回永久URL
        return generateAccessUrl(storagePath);
    }

    @Override
    public boolean deleteFile(String storagePath) {
        try {
            Path path = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            boolean deleted = Files.deleteIfExists(path);
            log.info("删除文件 - 路径: {}, 结果: {}", storagePath, deleted);
            return deleted;
        } catch (Exception e) {
            log.error("删除文件失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public int batchDeleteFiles(String[] storagePaths) {
        int deletedCount = 0;
        for (String storagePath : storagePaths) {
            if (deleteFile(storagePath)) {
                deletedCount++;
            }
        }
        log.info("批量删除文件完成 - 总数: {}, 成功: {}", storagePaths.length, deletedCount);
        return deletedCount;
    }

    @Override
    public boolean copyFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), sourcePath);
            Path target = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), targetPath);
            
            ensureDirectoryExists(targetPath);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("复制文件成功 - 源: {}, 目标: {}", sourcePath, targetPath);
            return true;
        } catch (Exception e) {
            log.error("复制文件失败 - 源: {}, 目标: {}, 错误: {}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean moveFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), sourcePath);
            Path target = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), targetPath);
            
            ensureDirectoryExists(targetPath);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("移动文件成功 - 源: {}, 目标: {}", sourcePath, targetPath);
            return true;
        } catch (Exception e) {
            log.error("移动文件失败 - 源: {}, 目标: {}, 错误: {}", sourcePath, targetPath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            Path path = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.exists(path);
        } catch (Exception e) {
            log.error("检查文件存在性失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public long getFileSize(String storagePath) {
        try {
            Path path = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.exists(path) ? Files.size(path) : -1;
        } catch (Exception e) {
            log.error("获取文件大小失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public long getLastModified(String storagePath) {
        try {
            Path path = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), storagePath);
            return Files.exists(path) ? Files.getLastModifiedTime(path).toMillis() : -1;
        } catch (Exception e) {
            log.error("获取文件修改时间失败 - 路径: {}, 错误: {}", storagePath, e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public StorageStatistics getStorageStatistics() {
        try {
            Path rootPath = Paths.get(storageProperties.getStorage().getLocal().getBasePath());
            if (!Files.exists(rootPath)) {
                return new StorageStatistics(0, 0, 0, 0);
            }
            
            long[] stats = new long[2]; // [fileCount, totalSize]
            
            Files.walk(rootPath)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            stats[0]++; // fileCount
                            stats[1] += Files.size(path); // totalSize
                        } catch (IOException e) {
                            log.warn("统计文件失败: {}", path, e);
                        }
                    });
            
            // 获取磁盘空间信息
            long usableSpace = Files.getFileStore(rootPath).getUsableSpace();
            long totalSpace = Files.getFileStore(rootPath).getTotalSpace();
            long usedSpace = totalSpace - usableSpace;
            
            return new StorageStatistics(stats[0], stats[1], usedSpace, usableSpace);
            
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
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
            Path rootPath = Paths.get(storageProperties.getStorage().getLocal().getBasePath());
            
            // 检查根目录是否存在和可写
            if (!Files.exists(rootPath)) {
                Files.createDirectories(rootPath);
            }
            
            // 创建测试文件
            Path testFile = rootPath.resolve("health_check.tmp");
            Files.write(testFile, "health check".getBytes());
            
            // 读取测试文件
            String content = new String(Files.readAllBytes(testFile));
            
            // 删除测试文件
            Files.deleteIfExists(testFile);
            
            return "health check".equals(content);
            
        } catch (Exception e) {
            log.error("存储服务健康检查失败", e);
            return false;
        }
    }

    // 私有辅助方法

    /**
     * 验证文件大小
     */
    private void validateFileSize(long fileSize) {
        if (fileSize <= 0) {
            throw new IllegalArgumentException("文件大小必须大于0");
        }
        long maxFileSize = storageProperties.getStorage().getLocal().getMaxFileSize();
        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + maxFileSize + " 字节");
        }
    }

    /**
     * 生成存储路径（按组织ID和日期分目录，实现数据隔离）
     */
    private String generateStoragePath(String fileId, String originalFilename, Long organizationId) {
        LocalDate now = LocalDate.now();
        String dateDir = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 获取文件扩展名
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 按组织ID隔离存储：/{orgId}/yyyy/MM/dd/{fileId}.ext
        return organizationId + "/" + dateDir + "/" + fileId + extension;
    }
    
    /**
     * 生成存储路径（兼容旧接口，使用默认组织ID）
     */
    private String generateStoragePath(String fileId, String originalFilename) {
        return generateStoragePath(fileId, originalFilename, 0L);
    }

    /**
     * 确保目录存在
     */
    private void ensureDirectoryExists(String filePath) throws IOException {
        Path fullPath = Paths.get(storageProperties.getStorage().getLocal().getBasePath(), filePath);
        Path parentDir = fullPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * 创建分片临时目录
     */
    private String createChunkTempDirectory(String uploadId) throws IOException {
        Path chunkTempDir = Paths.get(storageProperties.getStorage().getLocal().getTempPath() + "/chunks", uploadId);
        Files.createDirectories(chunkTempDir);
        return chunkTempDir.toString();
    }

    /**
     * 分片上传信息内部类
     */
    private static class ChunkUploadInfo {
        private final String uploadId;
        private final String filename;
        private final long fileSize;
        private final long chunkSize;
        private final int totalChunks;
        private final String chunkDir;
        private final List<Integer> uploadedChunks;

        public ChunkUploadInfo(String uploadId, String filename, long fileSize, 
                              long chunkSize, int totalChunks, String chunkDir) {
            this.uploadId = uploadId;
            this.filename = filename;
            this.fileSize = fileSize;
            this.chunkSize = chunkSize;
            this.totalChunks = totalChunks;
            this.chunkDir = chunkDir;
            this.uploadedChunks = new ArrayList<>();
        }

        public void markChunkUploaded(int chunkNumber) {
            if (!uploadedChunks.contains(chunkNumber)) {
                uploadedChunks.add(chunkNumber);
            }
        }

        // Getters
        public String getUploadId() { return uploadId; }
        public String getFilename() { return filename; }
        public long getFileSize() { return fileSize; }
        public long getChunkSize() { return chunkSize; }
        public int getTotalChunks() { return totalChunks; }
        public String getChunkDir() { return chunkDir; }
        public List<Integer> getUploadedChunks() { return uploadedChunks; }
    }
}
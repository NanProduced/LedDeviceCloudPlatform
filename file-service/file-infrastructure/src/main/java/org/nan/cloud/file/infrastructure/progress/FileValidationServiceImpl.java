package org.nan.cloud.file.infrastructure.progress;

import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.api.dto.SupportedFileTypesResponse;
import org.nan.cloud.file.api.enums.FileType;
import org.nan.cloud.file.application.service.FileValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 文件验证服务实现
 * 
 * 功能特性：
 * 1. 文件类型验证（基于MIME类型和文件扩展名双重检查）
 * 2. 文件大小限制（可配置不同类型的限制）
 * 3. 文件名安全检查（防止路径遍历、特殊字符等）
 * 4. 文件内容基础安全检查（文件头魔数验证）
 * 5. 黑名单扩展名过滤
 * 6. MIME类型检测
 * 
 * 开发环境配置：
 * - 宽松的文件类型限制，支持常见格式
 * - 合理的文件大小限制
 * - 基础的安全检查，不影响开发效率
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class FileValidationServiceImpl implements FileValidationService {

    // ==================== 配置参数 ====================
    
    /**
     * 默认最大文件大小
     */
    @Value("${file.validation.max-file-size.default:1073741824}")
    private long defaultMaxFileSize;
    
    /**
     * 图片文件最大大小
     */
    @Value("${file.validation.max-file-size.image:104857600}")
    private long imageMaxSize;
    
    /**
     * 视频文件最大大小
     */
    @Value("${file.validation.max-file-size.video:5368709120}")
    private long videoMaxSize;
    
    /**
     * 音频文件最大大小
     */
    @Value("${file.validation.max-file-size.audio:524288000}")
    private long audioMaxSize;
    
    /**
     * 文档文件最大大小
     */
    @Value("${file.validation.max-file-size.document:104857600}")
    private long documentMaxSize;
    
    /**
     * 是否启用严格MIME类型检查
     */
    @Value("${file.validation.strict-mime-check:false}")
    private boolean strictMimeCheck;
    
    /**
     * 是否启用病毒扫描（开发环境默认关闭）
     */
    @Value("${file.validation.virus-scan-enabled:false}")
    private boolean virusScanEnabled;

    // ==================== 静态配置 ====================
    
    /**
     * 支持的文件类型配置
     */
    private Map<FileType, Set<String>> supportedExtensions;
    private Map<FileType, Set<String>> supportedMimeTypes;
    private Map<FileType, Long> typeSizeLimits;
    
    /**
     * 黑名单文件扩展名
     */
    private Set<String> blacklistedExtensions;
    
    /**
     * 文件魔数映射（用于文件头验证）
     */
    private Map<String, String> fileMagicNumbers;
    
    /**
     * 文件名验证正则表达式
     */
    private Pattern filenamePattern;

    @PostConstruct
    public void initialize() {
        initializeSupportedTypes();
        initializeBlacklist();
        initializeFileMagicNumbers();
        initializeFilenamePattern();
        
        log.info("FileValidationService initialized - 默认大小限制: {}MB, 严格检查: {}, 病毒扫描: {}", 
                defaultMaxFileSize / 1024 / 1024, strictMimeCheck, virusScanEnabled);
    }

    @Override
    public FileValidationResult validate(MultipartFile file, FileUploadRequest request) {
        log.debug("开始验证文件 - 文件名: {}, 大小: {}, MIME: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // 1. 检查文件是否为空
            if (file.isEmpty()) {
                return FileValidationResult.failure("文件内容为空", ValidationErrorCode.EMPTY_FILE);
            }

            String filename = file.getOriginalFilename();
            if (filename == null || filename.trim().isEmpty()) {
                return FileValidationResult.failure("文件名不能为空", ValidationErrorCode.INVALID_FILENAME);
            }

            // 2. 验证文件名
            if (!validateFilename(filename)) {
                return FileValidationResult.failure("文件名包含非法字符或格式不正确", ValidationErrorCode.INVALID_FILENAME);
            }

            // 3. 检查黑名单扩展名
            if (isBlacklistedExtension(filename)) {
                return FileValidationResult.failure("文件类型不被允许", ValidationErrorCode.BLACKLISTED_EXTENSION);
            }

            // 4. 验证文件类型
            if (!validateFileType(file)) {
                return FileValidationResult.failure("不支持的文件类型", ValidationErrorCode.INVALID_FILE_TYPE);
            }

            // 5. 验证文件大小
            FileType detectedType = FileType.fromMimeType(file.getContentType());
            long maxSize = getMaxFileSizeForType(detectedType);
            if (!validateFileSize(file, maxSize)) {
                return FileValidationResult.failure(
                        String.format("文件大小超过限制，最大允许: %dMB", maxSize / 1024 / 1024), 
                        ValidationErrorCode.FILE_TOO_LARGE);
            }

            // 6. 验证文件内容安全性
            if (!validateFileSecurity(file)) {
                return FileValidationResult.failure("文件安全检查未通过", ValidationErrorCode.MALICIOUS_FILE);
            }

            // 7. 病毒扫描（如果启用）
            if (virusScanEnabled && hasVirus(file)) {
                return FileValidationResult.failure("检测到病毒或恶意软件", ValidationErrorCode.VIRUS_DETECTED);
            }

            log.debug("文件验证通过 - 文件名: {}", filename);
            return FileValidationResult.success();

        } catch (Exception e) {
            log.error("文件验证过程中发生异常 - 文件名: {}, 错误: {}", file.getOriginalFilename(), e.getMessage(), e);
            return FileValidationResult.failure("文件验证失败: " + e.getMessage(), "VALIDATION_ERROR");
        }
    }

    @Override
    public boolean validateFileType(MultipartFile file) {
        String mimeType = file.getContentType();
        String filename = file.getOriginalFilename();
        
        if (filename == null) {
            return false;
        }

        // 获取文件扩展名
        String extension = getFileExtension(filename).toLowerCase();
        
        // 基于扩展名判断文件类型
        FileType fileType = FileType.fromExtension(filename);
        
        // 检查扩展名是否受支持
        Set<String> allowedExtensions = supportedExtensions.get(fileType);
        if (allowedExtensions == null || !allowedExtensions.contains(extension)) {
            log.warn("不支持的文件扩展名 - 扩展名: {}, 类型: {}", extension, fileType);
            return false;
        }

        // 如果启用严格MIME检查
        if (strictMimeCheck && mimeType != null) {
            Set<String> allowedMimeTypes = supportedMimeTypes.get(fileType);
            if (allowedMimeTypes != null && !allowedMimeTypes.contains(mimeType.toLowerCase())) {
                log.warn("MIME类型不匹配 - 扩展名: {}, MIME: {}, 期望类型: {}", extension, mimeType, fileType);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean validateFileSize(MultipartFile file, long maxSize) {
        long fileSize = file.getSize();
        boolean valid = fileSize > 0 && fileSize <= maxSize;
        
        if (!valid) {
            log.warn("文件大小验证失败 - 文件大小: {}B, 最大限制: {}B", fileSize, maxSize);
        }
        
        return valid;
    }

    @Override
    public boolean validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        
        // 使用正则表达式验证文件名
        return filenamePattern.matcher(filename).matches();
    }

    @Override
    public boolean validateFileSecurity(MultipartFile file) {
        try {
            // 1. 检查文件头魔数
            byte[] header = new byte[Math.min(16, (int) file.getSize())];
            file.getInputStream().read(header);
            
            String headerHex = bytesToHex(header);
            
            // 2. 验证文件头是否匹配扩展名
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String extension = getFileExtension(filename).toLowerCase();
                String expectedMagic = fileMagicNumbers.get(extension);
                
                if (expectedMagic != null && !headerHex.startsWith(expectedMagic)) {
                    log.warn("文件头魔数不匹配 - 扩展名: {}, 期望: {}, 实际: {}", 
                            extension, expectedMagic, headerHex.substring(0, Math.min(8, headerHex.length())));
                    // 开发环境不严格检查魔数，仅记录警告
                    if (strictMimeCheck) {
                        return false;
                    }
                }
            }
            
            // 3. 检查文件是否可能是可执行文件
            if (isPotentiallyExecutable(headerHex)) {
                log.error("检测到可能的可执行文件 - 文件头: {}", headerHex.substring(0, Math.min(16, headerHex.length())));
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            log.error("文件安全检查失败", e);
            return false;
        }
    }

    @Override
    public String detectMimeType(MultipartFile file) {
        try {
            // 1. 优先使用文件内容检测
            byte[] header = new byte[Math.min(1024, (int) file.getSize())];
            file.getInputStream().read(header);
            
            // 基于文件头检测MIME类型
            String detectedMime = detectMimeTypeFromHeader(header);
            if (detectedMime != null) {
                return detectedMime;
            }
            
            // 2. 回退到文件扩展名检测
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String extension = getFileExtension(filename).toLowerCase();
                return getMimeTypeFromExtension(extension);
            }
            
            // 3. 最后回退到MultipartFile提供的MIME类型
            return file.getContentType();
            
        } catch (IOException e) {
            log.error("MIME类型检测失败", e);
            return file.getContentType();
        }
    }

    @Override
    public boolean isMaliciousFile(MultipartFile file) {
        try {
            // 简单的恶意文件检测逻辑
            String filename = file.getOriginalFilename();
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                
                // 检查双重扩展名
                if (hasDoubleExtension(lowerFilename)) {
                    log.warn("检测到双重扩展名 - 文件名: {}", filename);
                    return true;
                }
                
                // 检查常见恶意文件模式
                if (containsMaliciousPattern(lowerFilename)) {
                    log.warn("检测到恶意文件模式 - 文件名: {}", filename);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("恶意文件检测失败", e);
            return true; // 出错时保守处理
        }
    }

    @Override
    public boolean hasVirus(MultipartFile file) {
        if (!virusScanEnabled) {
            return false;
        }
        
        // TODO: 集成第三方病毒扫描引擎（如ClamAV）
        // 开发环境暂时返回false，生产环境应该集成真实的病毒扫描
        log.debug("病毒扫描跳过（开发环境）- 文件: {}", file.getOriginalFilename());
        return false;
    }

    @Override
    public SupportedFileTypesResponse getSupportedFileTypes() {
        Map<String, SupportedFileTypesResponse.FileTypeInfo> supportedTypes = new HashMap<>();
        Map<String, Long> sizeLimits = new HashMap<>();
        
        // 构建支持的文件类型信息
        for (FileType fileType : FileType.values()) {
            Set<String> extensions = supportedExtensions.get(fileType);
            Set<String> mimeTypes = supportedMimeTypes.get(fileType);
            Long maxSize = typeSizeLimits.get(fileType);
            
            if (extensions != null && !extensions.isEmpty()) {
                SupportedFileTypesResponse.FileTypeInfo typeInfo = SupportedFileTypesResponse.FileTypeInfo.builder()
                        .category(fileType.name())
                        .extensions(new ArrayList<>(extensions))
                        .mimeTypes(mimeTypes != null ? new ArrayList<>(mimeTypes) : new ArrayList<>())
                        .maxSize(maxSize)
                        .supportsTranscoding(fileType == FileType.VIDEO || fileType == FileType.AUDIO)
                        .supportsPreview(fileType == FileType.IMAGE || fileType == FileType.VIDEO)
                        .supportsThumbnail(fileType == FileType.IMAGE || fileType == FileType.VIDEO)
                        .description(fileType.getDescription())
                        .build();
                
                supportedTypes.put(fileType.getCode(), typeInfo);
                sizeLimits.put(fileType.getCode(), maxSize);
            }
        }
        
        // 系统配置
        SupportedFileTypesResponse.SystemConfig systemConfig = SupportedFileTypesResponse.SystemConfig.builder()
                .maxConcurrentUploads(5)
                .recommendedChunkSize(5L * 1024 * 1024) // 5MB
                .storageStrategies(Arrays.asList("LOCAL", "OSS"))
                .tempFileRetentionDays(7)
                .virusScanEnabled(virusScanEnabled)
                .contentRecognitionEnabled(false)
                .build();
        
        return SupportedFileTypesResponse.builder()
                .supportedTypes(supportedTypes)
                .sizeLimits(sizeLimits)
                .systemConfig(systemConfig)
                .build();
    }

    @Override
    public long getMaxFileSizeForType(FileType fileType) {
        return typeSizeLimits.getOrDefault(fileType, defaultMaxFileSize);
    }

    @Override
    public boolean isBlacklistedExtension(String filename) {
        if (filename == null) {
            return true;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        return blacklistedExtensions.contains(extension);
    }

    @Override
    public String normalizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "unnamed_file";
        }
        
        // 移除路径信息，只保留文件名
        String normalized = Paths.get(filename).getFileName().toString();
        
        // 替换特殊字符
        normalized = normalized.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // 限制长度
        if (normalized.length() > 200) {
            String extension = getFileExtension(normalized);
            String baseName = normalized.substring(0, 200 - extension.length() - 1);
            normalized = baseName + "." + extension;
        }
        
        return normalized;
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 初始化支持的文件类型
     */
    private void initializeSupportedTypes() {
        supportedExtensions = new HashMap<>();
        supportedMimeTypes = new HashMap<>();
        typeSizeLimits = new HashMap<>();
        
        // 图片类型
        supportedExtensions.put(FileType.IMAGE, Set.of(
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff"
        ));
        supportedMimeTypes.put(FileType.IMAGE, Set.of(
                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", 
                "image/svg+xml", "image/x-icon", "image/tiff"
        ));
        typeSizeLimits.put(FileType.IMAGE, imageMaxSize);
        
        // 视频类型
        supportedExtensions.put(FileType.VIDEO, Set.of(
                "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v", "3gp", "ts"
        ));
        supportedMimeTypes.put(FileType.VIDEO, Set.of(
                "video/mp4", "video/avi", "video/quicktime", "video/x-ms-wmv", 
                "video/x-flv", "video/x-matroska", "video/webm"
        ));
        typeSizeLimits.put(FileType.VIDEO, videoMaxSize);
        
        // 音频类型
        supportedExtensions.put(FileType.AUDIO, Set.of(
                "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus"
        ));
        supportedMimeTypes.put(FileType.AUDIO, Set.of(
                "audio/mpeg", "audio/wav", "audio/flac", "audio/aac", 
                "audio/ogg", "audio/x-ms-wma", "audio/mp4"
        ));
        typeSizeLimits.put(FileType.AUDIO, audioMaxSize);
        
        // 文档类型
        supportedExtensions.put(FileType.DOCUMENT, Set.of(
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf"
        ));
        supportedMimeTypes.put(FileType.DOCUMENT, Set.of(
                "application/pdf", "application/msword", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel", 
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain", "application/rtf"
        ));
        typeSizeLimits.put(FileType.DOCUMENT, documentMaxSize);
        
        // 压缩包类型
        supportedExtensions.put(FileType.ARCHIVE, Set.of(
                "zip", "rar", "7z", "tar", "gz"
        ));
        supportedMimeTypes.put(FileType.ARCHIVE, Set.of(
                "application/zip", "application/x-rar-compressed", 
                "application/x-7z-compressed", "application/x-tar", "application/gzip"
        ));
        typeSizeLimits.put(FileType.ARCHIVE, defaultMaxFileSize);
        
        // 其他类型
        supportedExtensions.put(FileType.OTHER, Set.of());
        supportedMimeTypes.put(FileType.OTHER, Set.of());
        typeSizeLimits.put(FileType.OTHER, defaultMaxFileSize);
    }
    
    /**
     * 初始化黑名单扩展名
     */
    private void initializeBlacklist() {
        blacklistedExtensions = Set.of(
                // 可执行文件
                "exe", "bat", "cmd", "com", "scr", "pif", "vbs", "js", "jar", "msi",
                // 脚本文件
                "php", "asp", "aspx", "jsp", "pl", "py", "rb", "sh",
                // 系统文件
                "sys", "dll", "drv", "ini", "reg",
                // 其他危险格式
                "hta", "wsf", "wsh"
        );
    }
    
    /**
     * 初始化文件魔数映射
     */
    private void initializeFileMagicNumbers() {
        fileMagicNumbers = Map.of(
                "jpg", "FFD8FF",
                "jpeg", "FFD8FF",
                "png", "89504E47",
                "gif", "474946",
                "pdf", "25504446",
                "zip", "504B0304",
                "mp4", "66747970"
        );
    }
    
    /**
     * 初始化文件名验证正则表达式
     */
    private void initializeFilenamePattern() {
        // 允许字母、数字、中文、常见符号，但排除路径符号和特殊字符
        filenamePattern = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z0-9._()\\[\\]{}~!@#$%^&+=,;' -]{1,200}\\.[a-zA-Z0-9]{1,10}$");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }
    
    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
    
    /**
     * 检查是否为可能的可执行文件
     */
    private boolean isPotentiallyExecutable(String headerHex) {
        return headerHex.startsWith("4D5A") || // PE executable (Windows)
               headerHex.startsWith("7F454C46") || // ELF executable (Linux)
               headerHex.startsWith("CAFEBABE") || // Java class file
               headerHex.startsWith("FEEDFACE"); // Mach-O executable (macOS)
    }
    
    /**
     * 基于文件头检测MIME类型
     */
    private String detectMimeTypeFromHeader(byte[] header) {
        String headerHex = bytesToHex(header);
        
        if (headerHex.startsWith("FFD8FF")) return "image/jpeg";
        if (headerHex.startsWith("89504E47")) return "image/png";
        if (headerHex.startsWith("474946")) return "image/gif";
        if (headerHex.startsWith("25504446")) return "application/pdf";
        if (headerHex.startsWith("504B0304")) return "application/zip";
        
        return null;
    }
    
    /**
     * 根据扩展名获取MIME类型
     */
    private String getMimeTypeFromExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
    
    /**
     * 检查是否有双重扩展名
     */
    private boolean hasDoubleExtension(String filename) {
        String[] parts = filename.split("\\.");
        return parts.length > 2;
    }
    
    /**
     * 检查是否包含恶意模式
     */
    private boolean containsMaliciousPattern(String filename) {
        String[] maliciousPatterns = {
                "script", "onload", "onerror", "javascript:", "vbscript:",
                "../", "..\\", "%2e%2e", "cmd.exe", "powershell"
        };
        
        for (String pattern : maliciousPatterns) {
            if (filename.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
}
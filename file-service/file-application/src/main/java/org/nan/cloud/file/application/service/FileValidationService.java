package org.nan.cloud.file.application.service;

import lombok.Data;
import org.nan.cloud.file.api.dto.FileUploadRequest;
import org.nan.cloud.file.api.dto.SupportedFileTypesResponse;
import org.nan.cloud.file.api.enums.FileType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件验证服务接口
 * 
 * 提供文件上传前的各种验证功能：
 * - 文件类型验证
 * - 文件大小限制
 * - 文件内容安全检查
 * - 文件名合法性验证
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public interface FileValidationService {

    /**
     * 验证文件
     * 
     * @param file 上传的文件
     * @param request 上传请求参数
     * @return 验证结果
     */
    FileValidationResult validate(MultipartFile file, FileUploadRequest request);

    /**
     * 验证文件类型
     * 
     * @param file 上传的文件
     * @return 是否通过验证
     */
    boolean validateFileType(MultipartFile file);

    /**
     * 验证文件大小
     * 
     * @param file 上传的文件
     * @param maxSize 最大允许大小（字节）
     * @return 是否通过验证
     */
    boolean validateFileSize(MultipartFile file, long maxSize);

    /**
     * 验证文件名
     * 
     * @param filename 文件名
     * @return 是否通过验证
     */
    boolean validateFilename(String filename);

    /**
     * 验证文件内容安全性
     * 
     * @param file 上传的文件
     * @return 是否通过验证
     */
    boolean validateFileSecurity(MultipartFile file);

    /**
     * 检测文件的真实MIME类型
     * 
     * @param file 上传的文件
     * @return 真实的MIME类型
     */
    String detectMimeType(MultipartFile file);

    /**
     * 检查是否为恶意文件
     * 
     * @param file 上传的文件
     * @return 是否为恶意文件
     */
    boolean isMaliciousFile(MultipartFile file);

    /**
     * 检查文件是否包含病毒
     * 
     * @param file 上传的文件
     * @return 是否包含病毒
     */
    boolean hasVirus(MultipartFile file);

    /**
     * 获取支持的文件类型
     * 
     * @return 支持的文件类型信息
     */
    SupportedFileTypesResponse getSupportedFileTypes();

    /**
     * 获取文件类型的最大尺寸限制
     * 
     * @param fileType 文件类型
     * @return 最大尺寸限制（字节）
     */
    long getMaxFileSizeForType(FileType fileType);

    /**
     * 检查文件扩展名是否在黑名单中
     * 
     * @param filename 文件名
     * @return 是否在黑名单中
     */
    boolean isBlacklistedExtension(String filename);

    /**
     * 规范化文件名
     * 
     * @param filename 原始文件名
     * @return 规范化后的文件名
     */
    String normalizeFilename(String filename);

    /**
     * 文件验证结果
     */
    @Data
    class FileValidationResult {
        private boolean valid;
        private String errorMessage;
        private String errorCode;
        private List<String> warnings;

        public FileValidationResult() {}

        public FileValidationResult(boolean valid) {
            this.valid = valid;
        }

        public FileValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public FileValidationResult(boolean valid, String errorMessage, String errorCode) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public static FileValidationResult success() {
            return new FileValidationResult(true);
        }

        public static FileValidationResult failure(String errorMessage) {
            return new FileValidationResult(false, errorMessage);
        }

        public static FileValidationResult failure(String errorMessage, String errorCode) {
            return new FileValidationResult(false, errorMessage, errorCode);
        }
    }


    /**
     * 验证错误代码常量
     */
    class ValidationErrorCode {
        public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String INVALID_FILENAME = "INVALID_FILENAME";
        public static final String MALICIOUS_FILE = "MALICIOUS_FILE";
        public static final String VIRUS_DETECTED = "VIRUS_DETECTED";
        public static final String BLACKLISTED_EXTENSION = "BLACKLISTED_EXTENSION";
        public static final String EMPTY_FILE = "EMPTY_FILE";
        public static final String CORRUPTED_FILE = "CORRUPTED_FILE";
    }
}
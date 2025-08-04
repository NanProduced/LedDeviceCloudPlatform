package org.nan.cloud.file.api.enums;

/**
 * 文件类型枚举
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
public enum FileType {
    IMAGE("image", "图片"),
    VIDEO("video", "视频"),
    AUDIO("audio", "音频"),
    DOCUMENT("document", "文档"),
    ARCHIVE("archive", "压缩包"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    FileType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { 
        return code; 
    }
    
    public String getDescription() { 
        return description; 
    }

    /**
     * 根据MIME类型推断文件类型
     * 
     * @param mimeType MIME类型
     * @return 文件类型枚举
     */
    public static FileType fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return OTHER;
        }
        
        String type = mimeType.toLowerCase();
        
        if (type.startsWith("image/")) {
            return IMAGE;
        }
        if (type.startsWith("video/")) {
            return VIDEO;
        }
        if (type.startsWith("audio/")) {
            return AUDIO;
        }
        if (type.contains("pdf") || type.contains("document") || 
            type.contains("word") || type.contains("excel") || 
            type.contains("powerpoint") || type.contains("text")) {
            return DOCUMENT;
        }
        if (type.contains("zip") || type.contains("rar") || 
            type.contains("tar") || type.contains("7z")) {
            return ARCHIVE;
        }
        
        return OTHER;
    }

    /**
     * 根据文件扩展名推断文件类型
     * 
     * @param filename 文件名
     * @return 文件类型枚举
     */
    public static FileType fromExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return OTHER;
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        
        // 图片类型
        if (extension.matches("jpg|jpeg|png|gif|bmp|webp|svg|ico|tiff|tga")) {
            return IMAGE;
        }
        
        // 视频类型
        if (extension.matches("mp4|avi|mov|wmv|flv|mkv|webm|m4v|3gp|ts|mpg|mpeg|rmvb|rm")) {
            return VIDEO;
        }
        
        // 音频类型
        if (extension.matches("mp3|wav|flac|aac|ogg|wma|m4a|opus|amr")) {
            return AUDIO;
        }
        
        // 文档类型
        if (extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf|odt|ods|odp")) {
            return DOCUMENT;
        }
        
        // 压缩包类型
        if (extension.matches("zip|rar|7z|tar|gz|bz2|xz|lzma")) {
            return ARCHIVE;
        }
        
        return OTHER;
    }

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 扩展名（不包含点号）
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1);
    }
}
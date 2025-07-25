package org.nan.cloud.file.api.dto;

import lombok.Data;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件版本响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@Schema(description = "文件版本响应")
public class FileVersionResponse {

    /**
     * 文件ID
     */
    @Schema(description = "文件ID")
    private String fileId;

    /**
     * 当前版本
     */
    @Schema(description = "当前版本")
    private String currentVersion;

    /**
     * 版本列表
     */
    @Schema(description = "文件版本列表")
    private List<VersionInfo> versions;

    /**
     * 总版本数
     */
    @Schema(description = "总版本数")
    private Integer totalVersions;

    /**
     * 版本管理状态
     */
    @Schema(description = "版本管理状态")
    private String versioningStatus;

    /**
     * 版本信息
     */
    @Data
    @Builder
    @Schema(description = "版本信息")
    public static class VersionInfo {
        
        /**
         * 版本号
         */
        @Schema(description = "版本号")
        private String version;

        /**
         * 版本ID
         */
        @Schema(description = "版本ID")
        private String versionId;

        /**
         * 文件大小
         */
        @Schema(description = "文件大小")
        private Long fileSize;

        /**
         * 文件MD5
         */
        @Schema(description = "文件MD5")
        private String fileMd5;

        /**
         * 存储路径
         */
        @Schema(description = "存储路径")
        private String storagePath;

        /**
         * 创建时间
         */
        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        /**
         * 创建用户
         */
        @Schema(description = "创建用户")
        private String createUser;

        /**
         * 是否当前版本
         */
        @Schema(description = "是否当前版本")
        private Boolean isCurrent;

        /**
         * 变更说明
         */
        @Schema(description = "变更说明")
        private String changeDescription;

        /**
         * 版本标签
         */
        @Schema(description = "版本标签")
        private List<String> tags;

        /**
         * 下载URL
         */
        @Schema(description = "下载URL")
        private String downloadUrl;

        /**
         * 可恢复
         */
        @Schema(description = "是否可恢复")
        private Boolean restorable;
    }
}
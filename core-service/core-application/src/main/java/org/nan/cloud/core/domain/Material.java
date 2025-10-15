package org.nan.cloud.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Material {

    /**
     * 素材ID
     */
    private Long mid;

    /**
     * 素材文件名
     */
    private String materialName;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * 组织ID
     */
    private Long oid;

    /**
     * 所属用户组ID
     * 为null则说明在公共资源组
     */
    private Long ugid;

    /**
     * 所属文件夹ID
     * 1.存在ID则表示在文件夹中
     * 2.为null则说明在用户组下（未归属文件夹）或者在公共资源组
     */
    private Long fid;

    /**
     * 素材类型
     * IMAGE/VIDEO/AUDIO/DOCUMENT
     */
    private String materialType;

    /**
     * 素材描述
     */
    private String description;

    /**
     * 使用次数
     */
    private Long usageCount;

    /**
     * 上传者ID
     */
    private Long uploadedBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // === 文件相关信息 ===

    /**
     * 文件MD5哈希值
     */
    private String md5Hash;

    /**
     * 原始文件大小（字节）
     */
    private Long originalFileSize;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 文件扩展名
     */
    private String fileExtension;

    /**
     * 存储类型
     * LOCAL/ALIYUN_OSS
     */
    private String storageType;

    /**
     * 存储路径（来自 material_file 联表字段）
     */
    private String storagePath;

    /**
     * 首次上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 引用计数，用于垃圾回收
     */
    private Long refCount;

    /**
     * 文件状态
     * 1:已完成, 2:处理中, 3:失败
     */
    private Integer fileStatus;

    /**
     * 文件元数据ID（MongoDB ObjectId）
     * 用于存储分辨率、时长等详细元数据
     */
    private String metaDataId;

    /**
     * 是否为公共资源
     */
    public boolean isPublicResource() {
        return ugid == null;
    }

    /**
     * 是否在文件夹中
     */
    public boolean isInFolder() {
        return fid != null;
    }

    /**
     * 格式化文件大小
     */
    public String getFormattedFileSize() {
        if (originalFileSize == null || originalFileSize == 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = originalFileSize;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * 获取文件状态描述
     */
    public String getFileStatusDescription() {
        if (fileStatus == null) {
            return "未知";
        }
        return switch (fileStatus) {
            case 1 -> "已完成";
            case 2 -> "处理中";
            case 3 -> "失败";
            default -> "未知";
        };
    }

    /**
     * 转码来源素材ID
     */
    private Long sourceMaterialId;

    /**
     * 转码预设名/代码
     */
    private String transcodePreset;
}
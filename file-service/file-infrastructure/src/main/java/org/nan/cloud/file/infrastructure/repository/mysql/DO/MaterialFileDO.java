package org.nan.cloud.file.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件信息数据对象
 * 
 * 对应数据库表：material_file
 * 与core-service的MaterialFileDO保持一致的表结构
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("material_file")
public class MaterialFileDO {

    /**
     * 文件ID（主键）
     */
    @TableId(value = "file_id", type = IdType.ASSIGN_UUID)
    private String fileId;

    /**
     * MD5哈希值（用于去重）
     */
    @TableField("md5_hash")
    private String md5Hash;

    /**
     * 文件大小（字节）
     */
    @TableField("original_file_size")
    private Long originalFileSize;

    /**
     * MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件扩展名
     */
    @TableField("file_extension")
    private String fileExtension;

    /**
     * 存储类型（LOCAL/ALIYUN_OSS）
     */
    @TableField("storage_type")
    private String storageType;

    /**
     * 存储路径
     */
    @TableField("storage_path")
    private String storagePath;

    /**
     * 首次上传时间
     */
    @TableField("upload_time")
    private LocalDateTime uploadTime;

    /**
     * 引用计数，用于垃圾回收
     */
    @TableField("ref_count")
    private Long refCount;

    /**
     * 文件状态（1:已完成, 2:处理中, 3:失败）
     */
    @TableField("file_status")
    private Integer fileStatus;

    /**
     * 缩略图路径
     */
    @TableField("thumbnail_path")
    private String thumbnailPath;

    /**
     * 文件元数据ID（MongoDB ObjectId）
     */
    @TableField("meta_data_id")
    private String metadataId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除, 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
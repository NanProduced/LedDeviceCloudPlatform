package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("material_file")
public class MaterialFileDO {

    @TableId(value = "file_id", type = IdType.ASSIGN_UUID)
    private String fileId;

    @TableField("md5_hash")
    private String md5Hash;

    @TableField("original_file_size")
    private Long originalFileSize;

    @TableField("mime_type")
    private String mimeType;

    @TableField("file_extension")
    private String fileExtension;

    /**
     * LOCAL/ALIYUN_OSS
     */
    @TableField("storage_type")
    private String storageType;

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

    @TableField("file_status")
    private Integer fileStatus;

    /**
     * 缩略图路径
     */
    @TableField("thumbnail_path")
    private String thumbnailPath;

    /**
     * 文件元数据（分辨率、时长等）
     * MongoDB存储元数据，这里是MongoDB的objectId
     */
    @TableField("meta_data_id")
    private String metaDataId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

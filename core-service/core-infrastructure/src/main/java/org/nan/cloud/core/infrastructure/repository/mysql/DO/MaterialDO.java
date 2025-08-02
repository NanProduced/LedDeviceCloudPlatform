package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("material")
public class MaterialDO {

    @TableId(value = "mid", type = IdType.AUTO)
    private Long mid;

    @TableField("material_name")
    private String materialName;

    /**
     * 关联的文件ID
     */
    @TableField("file_id")
    private String file_id;

    @TableField("oid")
    private Long oid;

    /**
     * 所属用户组
     * 为null则说明在公共资源组
     */
    @TableField("ugid")
    private Long ugid;

    /**
     * 所属文件夹Id
     * 1.存在Id则表示在文件夹中
     * 2.为null则说明在用户组下（未归属文件夹）或者在公共资源组
     */
    @TableField("fid")
    private Long fid;

    /**
     * 素材类型
     * IMAGE/VIDEO/AUDIO/DOCUMENT
     */
    @TableField("material_type")
    private String materialType;

    @TableField("description")
    private String description;

    @TableField("usage_count")
    private Long usageCount;

    @TableField("uploaded_by")
    private Long uploadedBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;



}

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

/**
 * 文件夹DO
 */
@TableName("folder")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FolderDO {

    @TableId(value = "fid", type = IdType.AUTO)
    private Long fid;

    @TableField("folder_name")
    private String folderName;

    @TableField("oid")
    private Long oid;

    @TableField("ugid")
    private Long ugid;

    @TableField("description")
    private String description;

    /**
     * 1.子文件夹的父文件夹ID
     * 2.如果是PUBLIC，则为null
     * 3.用户组下根文件夹，为null
     */
    @TableField("parent")
    private Long parent;

    @TableField("path")
    private String path;

    /**
     * NORMAL: 普通文件夹
     * PUBLIC: 公共资源组
     */
    @TableField("folder_type")
    private String folderType;

    /**
     * 是否被分享
     */
    @TableField("shared")
    public boolean shared;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("updater_id")
    private Long updaterId;

    @TableField("update_time")
    private LocalDateTime updateTime;


}

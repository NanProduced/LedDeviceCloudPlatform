package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 素材资源分享关系DO
 *
 */
@Data
@TableName("material_share_rel")
public class MaterialShareRelDO {

    /**
     * 主键Id
     */
    @TableId(value = "share_id", type = IdType.AUTO)
    private Long shareId;

    /**
     * 资源id
     * 根据resourceType->fid或mid
     */
    @TableField("resource_id")
    private Long resourceId;

    /**
     * 1->素材文件
     * 2->素材文件夹
     */
    @TableField("resource_type")
    private Integer resourceType;

    /**
     * 共享到的用户组
     */
    @TableField("shared_to")
    private Long sharedTo;

    /**
     * 来自哪个用户组
     */
    @TableField("shared_from")
    private Long sharedFrom;

    @TableField("分享者")
    private Long sharedBy;

    @TableField("分享时间")
    private LocalDateTime sharedTime;
}

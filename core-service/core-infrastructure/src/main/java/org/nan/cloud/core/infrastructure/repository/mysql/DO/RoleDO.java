package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("role")
@Data
public class RoleDO {

    @TableId(value = "rid", type = IdType.AUTO)
    private Long rid;

    @TableField("oid")
    private Long oid;

    @TableField("name")
    private String name;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;

    @TableField("type")
    private Integer type;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("updater_id")
    private Long updaterId;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

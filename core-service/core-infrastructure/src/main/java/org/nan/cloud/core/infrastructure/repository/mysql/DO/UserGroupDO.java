package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_group")
public class UserGroupDO {

    @TableId(value = "ugid", type = IdType.AUTO)
    private Long ugid;

    @TableField("name")
    private String name;

    @TableField("oid")
    private Long oid;

    @TableField("parent")
    private Long parent;

    @TableField("path")
    private String path;

    @TableField("ug_type")
    private Integer ugType;

    @TableField("description")
    private String description;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;
}

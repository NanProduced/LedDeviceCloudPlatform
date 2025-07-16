package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("permission")
@Data
public class PermissionDO {

    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    @TableField("name")
    private String name;

    @TableField("url")
    private String url;

    @TableField("method")
    private String method;

    @TableField("description")
    private String description;

    @TableField("permission_group")
    private String group;

    @TableField("permission_type")
    private Integer type;
}

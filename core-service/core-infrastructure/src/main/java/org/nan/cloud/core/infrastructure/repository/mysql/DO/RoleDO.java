package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("A_role")
@Data
public class RoleDO {

    @TableId(value = "rid", type = IdType.AUTO)
    private Long rid;

    @TableField("oid")
    private Long oid;

    @TableField("name")
    private String name;

    @TableField("type")
    private Integer type;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;
}

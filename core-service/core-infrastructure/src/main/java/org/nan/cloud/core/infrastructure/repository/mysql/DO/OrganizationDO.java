package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("A_organization")
public class OrganizationDO {

    @TableId(value = "oid", type = IdType.AUTO)
    private Long oid;

    @TableField("name")
    private String name;

    @TableField("remark")
    private String remark;

    @TableField("root_t_group")
    private Long rootTgid;

    @TableField("root_u_group")
    private Long rootUgid;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("creator_id")
    private Long creatorId;
}

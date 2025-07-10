package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class UserDO {

    @TableId(value = "uid", type = IdType.AUTO)
    private Long uid;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("ugid")
    private Long ugid;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("status")
    private Integer status;

    @TableField("type")
    private Integer type;

    @TableField("oid")
    private Long oid;

    @TableField("suffix")
    private Long suffix;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

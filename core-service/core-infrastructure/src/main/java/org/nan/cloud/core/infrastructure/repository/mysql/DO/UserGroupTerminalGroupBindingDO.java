package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_group_terminal_group_rel")
public class UserGroupTerminalGroupBindingDO {

    @TableId(value = "binding_id", type = IdType.AUTO)
    private Long bindingId;

    @TableField("ugid")
    private Long ugid;

    @TableField("tgid")
    private Long tgid;

    @TableField("include_sub")
    private Boolean includeSub;

    @TableField("oid")
    private Long oid;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("updater_id")
    private Long updaterId;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
package org.nan.cloud.core.infrastructure.repository.mysql.DO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.nan.cloud.common.basic.model.BindingType;
import org.nan.cloud.core.infrastructure.config.BindingTypeHandler;

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

    @TableField(value = "binding_type", typeHandler = BindingTypeHandler.class)
    private BindingType bindingType;

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
    
    // 以下字段用于联表查询，不存储到数据库
    @TableField(exist = false)
    private String terminalGroupName;
    
    @TableField(exist = false)
    private String terminalGroupPath;
    
    @TableField(exist = false)
    private Long parentTgid;
    
    @TableField(exist = false)
    private Integer depth;
    
    @TableField(exist = false)
    private Integer childCount;
}
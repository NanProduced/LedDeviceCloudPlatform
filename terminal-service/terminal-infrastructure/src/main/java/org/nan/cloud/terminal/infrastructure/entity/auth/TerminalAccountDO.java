package org.nan.cloud.terminal.infrastructure.entity.auth;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 终端设备账号实体
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@TableName("terminal_account")
public class TerminalAccountEntity {

    /**
     * 主键ID
     */
    @TableId(value = "tid", type = IdType.AUTO)
    private Long tid;

    /**
     * 设备名称
     */
    @TableField("account")
    private String account;

    /**
     * 设备密码 - BCrypt加密
     */
    @TableField("password")
    private String password;

    /**
     * 设备状态：ACTIVE-活跃，INACTIVE-非活跃，DISABLED-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 组织ID
     */
    @TableField("oid")
    private Long oid;

    /**
     * 最后登录时间
     */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 更新者
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 版本号 - 乐观锁
     */
    @Version
    @TableField("version")
    private Integer version;
}
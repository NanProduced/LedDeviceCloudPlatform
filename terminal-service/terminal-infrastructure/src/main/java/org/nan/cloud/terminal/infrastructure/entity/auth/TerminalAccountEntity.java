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
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 设备ID - 唯一标识
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 设备名称
     */
    @TableField("device_name")
    private String deviceName;

    /**
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 设备密码 - BCrypt加密
     */
    @TableField("password")
    private String password;

    /**
     * 设备状态：ACTIVE-活跃，INACTIVE-非活跃，DISABLED-禁用
     */
    @TableField("status")
    private String status;

    /**
     * 组织ID
     */
    @TableField("organization_id")
    private String organizationId;

    /**
     * 组织名称
     */
    @TableField("organization_name")
    private String organizationName;

    /**
     * 设备描述
     */
    @TableField("description")
    private String description;

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
     * 登录失败次数
     */
    @TableField("failed_attempts")
    private Integer failedAttempts;

    /**
     * 是否锁定
     */
    @TableField("locked")
    private Boolean locked;

    /**
     * 锁定时间
     */
    @TableField("locked_time")
    private LocalDateTime lockedTime;

    /**
     * 锁定过期时间
     */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;

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
    private String createBy;

    /**
     * 更新者
     */
    @TableField("update_by")
    private String updateBy;

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
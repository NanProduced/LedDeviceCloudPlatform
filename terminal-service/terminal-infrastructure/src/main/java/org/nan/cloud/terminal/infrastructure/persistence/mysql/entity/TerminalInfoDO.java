package org.nan.cloud.terminal.infrastructure.persistence.mysql.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 终端设备信息实体
 * 
 * MySQL存储设备基础结构化信息，包括：
 * - 设备身份标识：设备ID、设备名称、设备类型
 * - 组织归属：组织ID、设备分组、权限级别
 * - 认证信息：设备用户名、密码hash、认证状态
 * - 基础属性：创建时间、更新时间、逻辑删除标记
 * 
 * 与MongoDB存储的复杂设备详情互补，MySQL负责结构化查询和关联关系
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("terminal_info")
public class TerminalInfoDO {

    /**
     * 主键ID
     */
    @TableId(value = "tid")
    private Long tid;

    /**
     * 设备名称 - 用户友好的设备显示名称
     */
    @TableField("terminal_name")
    private String terminalName;

    /**
     * 终端描述
     */
    @TableField("description")
    private String description;

    /**
     * 设备型号- 播放盒型号
     */
    @TableField("terminal_model")
    private String terminalModel;

    /**
     * 组织ID - 设备所属组织的标识
     */
    @TableField("oid")
    private Long oid;

    /**
     * 终端组ID - 设备所属终端组
     */
    @TableField("tgid")
    private Long tgid;

    /**
     * 设备固件版本
     */
    @TableField("firmware_version")
    private String firmwareVersion;

    /**
     * 设备序列号
     */
    @TableField("serial_number")
    private String serialNumber;

    /**
     * 设备创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 设备更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建者ID
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 更新者ID
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 逻辑删除标记 - 0未删除，1已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 数据版本号 - 乐观锁控制
     */
    @Version
    @TableField("version")
    private Integer version;

}
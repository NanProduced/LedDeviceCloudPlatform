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
public class TerminalDeviceEntity {

    /**
     * 主键ID - 雪花算法生成
     */
    @TableId(value = "tid")
    private Long tid;

    /**
     * 设备名称 - 用户友好的设备显示名称
     */
    @TableField("terminal_name")
    private String terminalName;

    /**
     * 设备类型 - LED屏幕类型分类
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * 组织ID - 设备所属组织的标识
     */
    @TableField("organization_id")
    private String organizationId;

    /**
     * 设备分组 - 业务层面的设备分组管理
     */
    @TableField("device_group")
    private String deviceGroup;

    /**
     * 设备认证用户名 - 用于Basic Auth认证
     */
    @TableField("username")
    private String username;

    /**
     * 设备认证密码Hash - BCrypt加密存储
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 设备状态 - ACTIVE, INACTIVE, MAINTENANCE, OFFLINE
     */
    @TableField("status")
    private String status;

    /**
     * 设备IP地址 - 最后连接的IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 设备MAC地址 - 网络物理地址
     */
    @TableField("mac_address")
    private String macAddress;

    /**
     * 设备固件版本
     */
    @TableField("firmware_version")
    private String firmwareVersion;

    /**
     * 设备硬件版本
     */
    @TableField("hardware_version")
    private String hardwareVersion;

    /**
     * 设备序列号
     */
    @TableField("serial_number")
    private String serialNumber;

    /**
     * 设备安装位置描述
     */
    @TableField("location")
    private String location;

    /**
     * 设备标签 - JSON格式存储，用于分类和搜索
     */
    @TableField("tags")
    private String tags;

    /**
     * 设备备注信息
     */
    @TableField("remarks")
    private String remarks;

    /**
     * 最后在线时间
     */
    @TableField("last_online_time")
    private LocalDateTime lastOnlineTime;

    /**
     * 最后离线时间
     */
    @TableField("last_offline_time")
    private LocalDateTime lastOfflineTime;

    /**
     * 累计在线时长(秒)
     */
    @TableField("total_online_duration")
    private Long totalOnlineDuration;

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

    /**
     * 设备状态枚举
     */
    public enum DeviceStatus {
        /**
         * 活跃状态 - 设备正常运行
         */
        ACTIVE("ACTIVE", "活跃"),
        
        /**
         * 非活跃状态 - 设备已停用但可恢复
         */
        INACTIVE("INACTIVE", "非活跃"),
        
        /**
         * 维护状态 - 设备正在维护中
         */
        MAINTENANCE("MAINTENANCE", "维护中"),
        
        /**
         * 离线状态 - 设备长时间未连接
         */
        OFFLINE("OFFLINE", "离线");

        private final String code;
        private final String description;

        DeviceStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static DeviceStatus fromCode(String code) {
            for (DeviceStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("未知的设备状态代码: " + code);
        }
    }

    /**
     * 检查设备是否在线
     * 基于最后在线时间判断（2分钟内有活动认为在线）
     */
    public boolean isOnline() {
        if (lastOnlineTime == null) {
            return false;
        }
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);
        return lastOnlineTime.isAfter(threshold) && 
               DeviceStatus.ACTIVE.getCode().equals(status);
    }

    /**
     * 计算设备空闲时间（秒）
     */
    public long getIdleTimeSeconds() {
        if (lastOnlineTime == null) {
            return Long.MAX_VALUE;
        }
        
        return java.time.Duration.between(lastOnlineTime, LocalDateTime.now()).getSeconds();
    }

    /**
     * 获取设备显示名称
     * 优先使用设备名称，没有则使用设备ID
     */
    public String getDisplayName() {
        return deviceName != null && !deviceName.trim().isEmpty() ? 
               deviceName : deviceId;
    }
}
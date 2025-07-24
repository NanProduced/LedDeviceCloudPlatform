package org.nan.cloud.terminal.api.dto.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备状态上报DTO
 * 
 * 设备定期向服务端上报运行状态，对应WordPress REST API格式：
 * PUT /wp-json/screen/v1/status
 * 
 * 请求体格式：
 * {
 *   "device_id": "device_001",
 *   "status": "online",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "system_info": { ... },
 *   "network_info": { ... },
 *   "application_info": { ... }
 * }
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class DeviceStatusDTO {

    /**
     * 设备ID
     */
    @JsonProperty("device_id")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 设备状态
     * online: 在线正常
     * offline: 离线
     * error: 异常
     * maintenance: 维护中
     */
    @JsonProperty("status")
    @NotBlank(message = "设备状态不能为空")
    private String status;

    /**
     * 状态时间戳
     */
    @JsonProperty("timestamp")
    @NotNull(message = "时间戳不能为空")
    private LocalDateTime timestamp;

    /**
     * 设备位置信息
     */
    @JsonProperty("location")
    private LocationInfo location;

    /**
     * 系统信息
     */
    @JsonProperty("system_info")
    private SystemInfo systemInfo;

    /**
     * 网络信息
     */
    @JsonProperty("network_info")
    private NetworkInfo networkInfo;

    /**
     * 应用信息
     */
    @JsonProperty("application_info")
    private ApplicationInfo applicationInfo;

    /**
     * 硬件信息
     */
    @JsonProperty("hardware_info")
    private HardwareInfo hardwareInfo;

    /**
     * 错误信息（当status为error时）
     */
    @JsonProperty("errors")
    private List<ErrorInfo> errors;

    /**
     * 告警信息
     */
    @JsonProperty("alerts")
    private List<AlertInfo> alerts;

    /**
     * 扩展属性
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * 位置信息
     */
    @Data
    public static class LocationInfo {
        
        /**
         * 经度
         */
        @JsonProperty("longitude")
        private Double longitude;

        /**
         * 纬度
         */
        @JsonProperty("latitude")
        private Double latitude;

        /**
         * 地址描述
         */
        @JsonProperty("address")
        private String address;

        /**
         * 楼层
         */
        @JsonProperty("floor")
        private String floor;

        /**
         * 区域
         */
        @JsonProperty("area")
        private String area;
    }

    /**
     * 系统信息
     */
    @Data
    public static class SystemInfo {
        
        /**
         * 操作系统类型
         */
        @JsonProperty("os_type")
        private String osType;

        /**
         * 操作系统版本
         */
        @JsonProperty("os_version")
        private String osVersion;

        /**
         * 系统架构
         */
        @JsonProperty("architecture")
        private String architecture;

        /**
         * 启动时间
         */
        @JsonProperty("boot_time")
        private LocalDateTime bootTime;

        /**
         * 运行时间（秒）
         */
        @JsonProperty("uptime_seconds")
        private Long uptimeSeconds;

        /**
         * CPU使用率（百分比）
         */
        @JsonProperty("cpu_usage")
        private Double cpuUsage;

        /**
         * 内存使用率（百分比）
         */
        @JsonProperty("memory_usage")
        private Double memoryUsage;

        /**
         * 总内存（MB）
         */
        @JsonProperty("total_memory_mb")
        private Long totalMemoryMb;

        /**
         * 可用内存（MB）
         */
        @JsonProperty("free_memory_mb")
        private Long freeMemoryMb;

        /**
         * 存储使用率（百分比）
         */
        @JsonProperty("storage_usage")
        private Double storageUsage;

        /**
         * 总存储空间（MB）
         */
        @JsonProperty("total_storage_mb")
        private Long totalStorageMb;

        /**
         * 可用存储空间（MB）
         */
        @JsonProperty("free_storage_mb")
        private Long freeStorageMb;
    }

    /**
     * 网络信息
     */
    @Data
    public static class NetworkInfo {
        
        /**
         * IP地址
         */
        @JsonProperty("ip_address")
        private String ipAddress;

        /**
         * MAC地址
         */
        @JsonProperty("mac_address")
        private String macAddress;

        /**
         * 网络类型（WiFi/Ethernet/4G等）
         */
        @JsonProperty("network_type")
        private String networkType;

        /**
         * 信号强度
         */
        @JsonProperty("signal_strength")
        private Integer signalStrength;

        /**
         * 网络速度（Mbps）
         */
        @JsonProperty("network_speed_mbps")
        private Double networkSpeedMbps;

        /**
         * 延迟（ms）
         */
        @JsonProperty("latency_ms")
        private Integer latencyMs;

        /**
         * 是否连接互联网
         */
        @JsonProperty("internet_connected")
        private Boolean internetConnected;

        /**
         * DNS服务器
         */
        @JsonProperty("dns_servers")
        private List<String> dnsServers;
    }

    /**
     * 应用信息
     */
    @Data
    public static class ApplicationInfo {
        
        /**
         * 应用版本
         */
        @JsonProperty("app_version")
        private String appVersion;

        /**
         * 构建版本
         */
        @JsonProperty("build_version")
        private String buildVersion;

        /**
         * 应用状态
         */
        @JsonProperty("app_status")
        private String appStatus;

        /**
         * 当前播放内容
         */
        @JsonProperty("current_content")
        private String currentContent;

        /**
         * 播放状态
         */
        @JsonProperty("playback_status")
        private String playbackStatus;

        /**
         * 最后更新时间
         */
        @JsonProperty("last_update_time")
        private LocalDateTime lastUpdateTime;

        /**
         * 配置版本
         */
        @JsonProperty("config_version")
        private String configVersion;

        /**
         * 活跃任务数
         */
        @JsonProperty("active_tasks")
        private Integer activeTasks;
    }

    /**
     * 硬件信息
     */
    @Data
    public static class HardwareInfo {
        
        /**
         * 设备型号
         */
        @JsonProperty("device_model")
        private String deviceModel;

        /**
         * 硬件版本
         */
        @JsonProperty("hardware_version")
        private String hardwareVersion;

        /**
         * 序列号
         */
        @JsonProperty("serial_number")
        private String serialNumber;

        /**
         * 屏幕分辨率
         */
        @JsonProperty("screen_resolution")
        private String screenResolution;

        /**
         * 屏幕状态
         */
        @JsonProperty("screen_status")
        private String screenStatus;

        /**
         * 温度（摄氏度）
         */
        @JsonProperty("temperature")
        private Double temperature;

        /**
         * 电源状态
         */
        @JsonProperty("power_status")
        private String powerStatus;

        /**
         * 传感器数据
         */
        @JsonProperty("sensors")
        private Map<String, Object> sensors;
    }

    /**
     * 错误信息
     */
    @Data
    public static class ErrorInfo {
        
        /**
         * 错误代码
         */
        @JsonProperty("error_code")
        private String errorCode;

        /**
         * 错误消息
         */
        @JsonProperty("error_message")
        private String errorMessage;

        /**
         * 错误级别
         */
        @JsonProperty("error_level")
        private String errorLevel;

        /**
         * 发生时间
         */
        @JsonProperty("occurred_at")
        private LocalDateTime occurredAt;

        /**
         * 错误次数
         */
        @JsonProperty("occurrence_count")
        private Integer occurrenceCount;
    }

    /**
     * 告警信息
     */
    @Data
    public static class AlertInfo {
        
        /**
         * 告警类型
         */
        @JsonProperty("alert_type")
        private String alertType;

        /**
         * 告警消息
         */
        @JsonProperty("alert_message")
        private String alertMessage;

        /**
         * 告警级别
         */
        @JsonProperty("alert_level")
        private String alertLevel;

        /**
         * 触发时间
         */
        @JsonProperty("triggered_at")
        private LocalDateTime triggeredAt;

        /**
         * 阈值
         */
        @JsonProperty("threshold")
        private Double threshold;

        /**
         * 当前值
         */
        @JsonProperty("current_value")
        private Double currentValue;
    }
}
package org.nan.cloud.terminal.infrastructure.persistence.mongodb.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备详情文档
 * 
 * MongoDB存储设备复杂配置和详情信息，包括：
 * - LED参数配置：分辨率、亮度、颜色校正等复杂参数
 * - 播放配置：播放列表、排程信息、音频设置等
 * - 网络配置：WiFi设置、代理配置、防火墙规则等
 * - 设备能力：支持的格式、硬件特性、扩展功能等
 * - 运行状态历史：性能指标、错误日志、状态变更记录
 * 
 * 与MySQL存储的基础信息互补，MongoDB负责复杂JSON文档存储和灵活查询
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Document(collection = "device_details")
public class DeviceDetailDocument {

    /**
     * 文档ID - MongoDB ObjectId
     */
    @Id
    private String id;

    /**
     * 设备ID - 与MySQL中的设备ID对应，建立关联
     */
    @Indexed(unique = true)
    @Field("device_id")
    private String deviceId;

    /**
     * 组织ID - 用于多租户数据隔离和查询
     */
    @Indexed
    @Field("organization_id")
    private String organizationId;

    /**
     * LED显示参数配置
     */
    @Field("led_config")
    private LedConfig ledConfig;

    /**
     * 播放控制配置
     */
    @Field("playback_config")
    private PlaybackConfig playbackConfig;

    /**
     * 网络连接配置
     */
    @Field("network_config")
    private NetworkConfig networkConfig;

    /**
     * 设备硬件能力描述
     */
    @Field("device_capabilities")
    private DeviceCapabilities deviceCapabilities;

    /**
     * 系统运行配置
     */
    @Field("system_config")
    private SystemConfig systemConfig;

    /**
     * 设备状态历史记录
     */
    @Field("status_history")
    private List<StatusHistoryEntry> statusHistory;

    /**
     * 性能监控数据
     */
    @Field("performance_metrics")
    private PerformanceMetrics performanceMetrics;

    /**
     * 自定义扩展属性
     */
    @Field("custom_attributes")
    private Map<String, Object> customAttributes;

    /**
     * 文档创建时间
     */
    @Indexed
    @Field("created_at")
    private LocalDateTime createdAt;

    /**
     * 文档更新时间
     */
    @Indexed
    @Field("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 数据版本号
     */
    @Field("version")
    private Integer version;

    /**
     * LED显示参数配置
     */
    @Data
    public static class LedConfig {
        /**
         * 屏幕分辨率 - 宽度x高度
         */
        private Resolution resolution;
        
        /**
         * 亮度设置 - 0-100
         */
        private Integer brightness;
        
        /**
         * 对比度设置 - 0-100
         */
        private Integer contrast;
        
        /**
         * 颜色校正参数
         */
        private ColorCorrection colorCorrection;
        
        /**
         * 刷新率 - Hz
         */
        private Integer refreshRate;
        
        /**
         * 色温设置 - Kelvin
         */
        private Integer colorTemperature;
        
        /**
         * 伽马值 - 显示曲线调整
         */
        private Double gamma;
        
        /**
         * 像素密度 - PPI
         */
        private Integer pixelDensity;
    }

    /**
     * 播放控制配置
     */
    @Data
    public static class PlaybackConfig {
        /**
         * 默认播放音量 - 0-100
         */
        private Integer defaultVolume;
        
        /**
         * 自动播放开关
         */
        private Boolean autoPlay;
        
        /**
         * 循环播放模式
         */
        private String loopMode;
        
        /**
         * 支持的媒体格式
         */
        private List<String> supportedFormats;
        
        /**
         * 播放列表配置
         */
        private PlaylistConfig playlistConfig;
        
        /**
         * 音频输出配置
         */
        private AudioConfig audioConfig;
    }

    /**
     * 网络连接配置
     */
    @Data
    public static class NetworkConfig {
        /**
         * WiFi配置
         */
        private WiFiConfig wifiConfig;
        
        /**
         * 以太网配置
         */
        private EthernetConfig ethernetConfig;
        
        /**
         * 代理服务器配置
         */
        private ProxyConfig proxyConfig;
        
        /**
         * 防火墙规则
         */
        private List<FirewallRule> firewallRules;
        
        /**
         * VPN配置
         */
        private VpnConfig vpnConfig;
    }

    /**
     * 设备硬件能力
     */
    @Data
    public static class DeviceCapabilities {
        /**
         * CPU信息
         */
        private CpuInfo cpuInfo;
        
        /**
         * 内存信息
         */
        private MemoryInfo memoryInfo;
        
        /**
         * 存储信息
         */
        private StorageInfo storageInfo;
        
        /**
         * 显示能力
         */
        private DisplayCapability displayCapability;
        
        /**
         * 网络接口能力
         */
        private List<NetworkInterface> networkInterfaces;
        
        /**
         * 扩展接口能力
         */
        private List<ExtensionInterface> extensionInterfaces;
    }

    /**
     * 系统运行配置
     */
    @Data
    public static class SystemConfig {
        /**
         * 系统时区
         */
        private String timezone;
        
        /**
         * 语言设置
         */
        private String language;
        
        /**
         * 日志级别
         */
        private String logLevel;
        
        /**
         * 自动更新开关
         */
        private Boolean autoUpdate;
        
        /**
         * 定时重启配置
         */
        private ScheduledReboot scheduledReboot;
        
        /**
         * 安全策略配置
         */
        private SecurityPolicy securityPolicy;
    }

    /**
     * 状态历史记录条目
     */
    @Data
    public static class StatusHistoryEntry {
        /**
         * 状态值
         */
        private String status;
        
        /**
         * 状态描述
         */
        private String description;
        
        /**
         * 时间戳
         */
        private LocalDateTime timestamp;
        
        /**
         * 触发原因
         */
        private String trigger;
        
        /**
         * 附加数据
         */
        private Map<String, Object> metadata;
    }

    /**
     * 性能监控数据
     */
    @Data
    public static class PerformanceMetrics {
        /**
         * CPU使用率历史
         */
        private List<MetricPoint> cpuUsage;
        
        /**
         * 内存使用率历史
         */
        private List<MetricPoint> memoryUsage;
        
        /**
         * 网络流量历史
         */
        private List<NetworkMetric> networkTraffic;
        
        /**
         * 温度监控历史
         */
        private List<MetricPoint> temperature;
        
        /**
         * 错误统计
         */
        private ErrorStatistics errorStatistics;
    }

    // 嵌套类定义省略详细实现，仅保留核心结构
    @Data public static class Resolution { private Integer width; private Integer height; }
    @Data public static class ColorCorrection { private Double redGain; private Double greenGain; private Double blueGain; }
    @Data public static class PlaylistConfig { private List<String> defaultPlaylist; private String playlistMode; }
    @Data public static class AudioConfig { private String outputDevice; private Boolean enableSurround; }
    @Data public static class WiFiConfig { private String ssid; private String password; private String security; }
    @Data public static class EthernetConfig { private String ipAddress; private String netmask; private String gateway; }
    @Data public static class ProxyConfig { private String host; private Integer port; private String username; }
    @Data public static class FirewallRule { private String protocol; private String sourceIp; private Integer port; private String action; }
    @Data public static class VpnConfig { private String serverAddress; private String username; private String protocol; }
    @Data public static class CpuInfo { private String model; private Integer cores; private Integer frequency; }
    @Data public static class MemoryInfo { private Long totalSize; private String type; private Integer frequency; }
    @Data public static class StorageInfo { private Long totalSize; private String type; private Long availableSpace; }
    @Data public static class DisplayCapability { private Resolution maxResolution; private List<String> supportedFormats; }
    @Data public static class NetworkInterface { private String name; private String type; private Integer speed; }
    @Data public static class ExtensionInterface { private String name; private String type; private Map<String, Object> properties; }
    @Data public static class ScheduledReboot { private String cronExpression; private Boolean enabled; }
    @Data public static class SecurityPolicy { private Boolean enableFirewall; private List<String> allowedIps; }
    @Data public static class MetricPoint { private LocalDateTime timestamp; private Double value; private String unit; }
    @Data public static class NetworkMetric { private LocalDateTime timestamp; private Long bytesIn; private Long bytesOut; }
    @Data public static class ErrorStatistics { private Integer totalErrors; private Integer criticalErrors; private LocalDateTime lastErrorTime; }

    /**
     * 初始化文档的默认值
     */
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        }
        
        // 初始化基础配置对象
        if (ledConfig == null) {
            ledConfig = new LedConfig();
        }
        if (playbackConfig == null) {
            playbackConfig = new PlaybackConfig();
        }
        if (networkConfig == null) {
            networkConfig = new NetworkConfig();
        }
        if (deviceCapabilities == null) {
            deviceCapabilities = new DeviceCapabilities();
        }
        if (systemConfig == null) {
            systemConfig = new SystemConfig();
        }
    }

    /**
     * 添加状态历史记录
     */
    public void addStatusHistory(String status, String description, String trigger) {
        if (statusHistory == null) {
            statusHistory = new java.util.ArrayList<>();
        }
        
        StatusHistoryEntry entry = new StatusHistoryEntry();
        entry.setStatus(status);
        entry.setDescription(description);
        entry.setTimestamp(LocalDateTime.now());
        entry.setTrigger(trigger);
        
        statusHistory.add(0, entry); // 添加到列表开头，保持时间倒序
        
        // 保持历史记录数量在合理范围内（最多保留100条）
        if (statusHistory.size() > 100) {
            statusHistory = statusHistory.subList(0, 100);
        }
    }

    /**
     * 更新文档版本和时间戳
     */
    public void updateVersion() {
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        } else {
            version++;
        }
    }
}
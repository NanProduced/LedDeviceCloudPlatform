package org.nan.cloud.message.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户状态响应
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@Schema(description = "用户状态响应")
public class UserStatusResponse {
    
    @Schema(description = "用户ID")
    private String userId;
    
    @Schema(description = "是否在线")
    private boolean online;

    @Schema(description = "连接数量")
    private Integer connectionCount;
    
    @Schema(description = "组织ID")
    private String organizationId;
    
    @Schema(description = "设备数量")
    private int deviceCount;
    
    @Schema(description = "在线设备列表")
    private List<DeviceInfo> devices;
    
    @Schema(description = "在线时长（分钟）")
    private int onlineDurationMinutes;
    
    @Schema(description = "最后更新时间")
    private LocalDateTime lastUpdateTime;
    
    @Schema(description = "状态信息")
    private String message;
    
    /**
     * 设备信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设备信息")
    public static class DeviceInfo {
        
        @Schema(description = "会话ID")
        private String sessionId;
        
        @Schema(description = "设备类型")
        private String deviceType;
        
        @Schema(description = "IP地址")
        private String ipAddress;
        
        @Schema(description = "用户代理")
        private String userAgent;
        
        @Schema(description = "连接时间")
        private LocalDateTime connectTime;
        
        @Schema(description = "最后心跳时间")
        private LocalDateTime lastHeartbeatTime;
    }
    
    /**
     * 组织统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "组织统计信息")
    public static class OrganizationStats {
        
        @Schema(description = "组织ID")
        private String organizationId;
        
        @Schema(description = "在线用户数")
        private int onlineUserCount;
        
        @Schema(description = "总连接数")
        private int totalConnections;
        
        @Schema(description = "最后更新时间")
        private LocalDateTime lastUpdateTime;
    }
    
    /**
     * 平台统计信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "平台统计信息")
    public static class PlatformStats {
        
        @Schema(description = "总在线用户数")
        private int totalOnlineUsers;
        
        @Schema(description = "总连接数")
        private int totalConnections;
        
        @Schema(description = "活跃组织数")
        private int activeOrganizations;
        
        @Schema(description = "最后更新时间")
        private LocalDateTime lastUpdateTime;
    }
    
    /**
     * 用户活跃度统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户活跃度统计")
    public static class ActivityStats {
        
        @Schema(description = "用户ID")
        private String userId;
        
        @Schema(description = "统计时间范围（小时）")
        private int hours;
        
        @Schema(description = "总活动数")
        private int totalActivities;
        
        @Schema(description = "各类活动统计")
        private Map<String, Integer> actionCounts;
        
        @Schema(description = "最后更新时间")
        private LocalDateTime lastUpdateTime;
    }
    
    /**
     * 组织活跃度统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "组织活跃度统计")
    public static class OrganizationActivityStats {
        
        @Schema(description = "组织ID")
        private String organizationId;
        
        @Schema(description = "统计时间范围（小时）")
        private int hours;
        
        @Schema(description = "总活动数")
        private int totalActivities;
        
        @Schema(description = "活跃用户数")
        private int activeUserCount;
        
        @Schema(description = "各类活动统计")
        private Map<String, Integer> actionCounts;
        
        @Schema(description = "最后更新时间")
        private LocalDateTime lastUpdateTime;
    }
    
    /**
     * 在线历史记录
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "在线历史记录")
    public static class OnlineHistory {
        
        @Schema(description = "用户ID")
        private String userId;
        
        @Schema(description = "上线时间")
        private LocalDateTime onlineTime;
        
        @Schema(description = "下线时间")
        private LocalDateTime offlineTime;
        
        @Schema(description = "在线时长（分钟）")
        private int durationMinutes;
        
        @Schema(description = "设备类型")
        private String deviceType;
        
        @Schema(description = "IP地址")
        private String ipAddress;
        
        @Schema(description = "离线原因")
        private String offlineReason;
    }
    
    /**
     * 状态变更事件
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "状态变更事件")
    public static class StatusChangeEvent {
        
        @Schema(description = "用户ID")
        private String userId;
        
        @Schema(description = "旧状态")
        private UserStatusResponse oldStatus;
        
        @Schema(description = "新状态")
        private UserStatusResponse newStatus;
        
        @Schema(description = "变更原因")
        private String changeReason;
        
        @Schema(description = "变更时间")
        private LocalDateTime timestamp;
    }
}
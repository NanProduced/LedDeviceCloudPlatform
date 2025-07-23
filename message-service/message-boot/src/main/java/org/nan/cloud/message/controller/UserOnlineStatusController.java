package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.UserStatusResponse;
import org.nan.cloud.message.service.UserOnlineStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户在线状态管理控制器
 * 
 * 提供用户在线状态查询、统计分析等功能的REST API接口。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user-online-status")
@RequiredArgsConstructor
@Tag(name = "用户在线状态API", description = "用户在线状态管理相关接口")
public class UserOnlineStatusController {
    
    private final UserOnlineStatusService userOnlineStatusService;
    
    // ==================== 基础状态查询 ====================
    
    /**
     * 检查用户是否在线
     */
    @GetMapping("/check/{userId}")
    @Operation(summary = "检查用户是否在线", description = "快速检查指定用户的在线状态")
    public ResponseEntity<Map<String, Object>> checkUserOnline(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId) {
        
        log.debug("检查用户在线状态: userId={}", userId);
        
        boolean isOnline = userOnlineStatusService.isUserOnline(userId);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "online", isOnline,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 获取用户详细在线状态
     */
    @GetMapping("/status/{userId}")
    @Operation(summary = "获取用户详细在线状态", description = "获取用户完整的在线状态信息，包括设备信息")
    public ResponseEntity<UserStatusResponse> getUserOnlineStatus(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId) {
        
        log.debug("获取用户详细在线状态: userId={}", userId);
        
        UserStatusResponse status = userOnlineStatusService.getUserOnlineStatus(userId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * 批量查询用户在线状态
     */
    @PostMapping("/status/batch")
    @Operation(summary = "批量查询用户在线状态", description = "一次查询多个用户的在线状态")
    public ResponseEntity<Map<String, UserStatusResponse>> batchGetUserOnlineStatus(
            @Parameter(description = "用户ID列表", required = true)
            @RequestBody List<String> userIds) {
        
        log.debug("批量查询用户在线状态: userCount={}", userIds.size());
        
        Map<String, UserStatusResponse> statusMap = 
            userOnlineStatusService.batchGetUserOnlineStatus(userIds);
        
        return ResponseEntity.ok(statusMap);
    }
    
    /**
     * 获取用户在线设备列表
     */
    @GetMapping("/devices/{userId}")
    @Operation(summary = "获取用户在线设备", description = "获取指定用户的所有在线设备信息")
    public ResponseEntity<List<UserStatusResponse.DeviceInfo>> getUserOnlineDevices(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId) {
        
        log.debug("获取用户在线设备: userId={}", userId);
        
        List<UserStatusResponse.DeviceInfo> devices = 
            userOnlineStatusService.getUserOnlineDevices(userId);
        
        return ResponseEntity.ok(devices);
    }
    
    // ==================== 组织维度统计 ====================
    
    /**
     * 获取组织在线用户列表
     */
    @GetMapping("/organization/{organizationId}/users")
    @Operation(summary = "获取组织在线用户", description = "获取指定组织的所有在线用户列表")
    public ResponseEntity<List<UserStatusResponse>> getOrganizationOnlineUsers(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("organizationId") String organizationId,
            @Parameter(description = "是否包含子组织", required = false)
            @RequestParam(value = "includeSubOrg", defaultValue = "false") boolean includeSubOrg) {
        
        log.debug("获取组织在线用户: organizationId={}, includeSubOrg={}", organizationId, includeSubOrg);
        
        List<UserStatusResponse> onlineUsers = 
            userOnlineStatusService.getOrganizationOnlineUsers(organizationId, includeSubOrg);
        
        return ResponseEntity.ok(onlineUsers);
    }
    
    /**
     * 获取组织在线统计
     */
    @GetMapping("/organization/{organizationId}/stats")
    @Operation(summary = "获取组织在线统计", description = "获取指定组织的在线用户统计信息")
    public ResponseEntity<UserStatusResponse.OrganizationStats> getOrganizationOnlineStats(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("organizationId") String organizationId) {
        
        log.debug("获取组织在线统计: organizationId={}", organizationId);
        
        UserStatusResponse.OrganizationStats stats = 
            userOnlineStatusService.getOrganizationOnlineStats(organizationId);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取平台总体在线统计
     */
    @GetMapping("/platform/stats")
    @Operation(summary = "获取平台在线统计", description = "获取整个平台的在线用户统计信息")
    public ResponseEntity<UserStatusResponse.PlatformStats> getPlatformOnlineStats() {
        
        log.debug("获取平台在线统计");
        
        UserStatusResponse.PlatformStats stats = 
            userOnlineStatusService.getPlatformOnlineStats();
        
        return ResponseEntity.ok(stats);
    }
    
    // ==================== 活跃度分析 ====================
    
    /**
     * 获取用户活跃度统计
     */
    @GetMapping("/activity/{userId}")
    @Operation(summary = "获取用户活跃度统计", description = "获取指定用户的活跃度统计信息")
    public ResponseEntity<UserStatusResponse.ActivityStats> getUserActivityStats(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "统计时间范围（小时）", required = false)
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        
        log.debug("获取用户活跃度统计: userId={}, hours={}", userId, hours);
        
        UserStatusResponse.ActivityStats stats = 
            userOnlineStatusService.getUserActivityStats(userId, hours);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 获取组织活跃度统计
     */
    @GetMapping("/activity/organization/{organizationId}")
    @Operation(summary = "获取组织活跃度统计", description = "获取指定组织的活跃度统计信息")
    public ResponseEntity<UserStatusResponse.OrganizationActivityStats> getOrganizationActivityStats(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("organizationId") String organizationId,
            @Parameter(description = "统计时间范围（小时）", required = false)
            @RequestParam(value = "hours", defaultValue = "24") int hours) {
        
        log.debug("获取组织活跃度统计: organizationId={}, hours={}", organizationId, hours);
        
        UserStatusResponse.OrganizationActivityStats stats = 
            userOnlineStatusService.getOrganizationActivityStats(organizationId, hours);
        
        return ResponseEntity.ok(stats);
    }
    
    // ==================== 管理功能 ====================
    
    /**
     * 更新用户心跳
     */
    @PostMapping("/heartbeat/{userId}/{sessionId}")
    @Operation(summary = "更新用户心跳", description = "更新指定用户会话的心跳时间")
    public ResponseEntity<Map<String, Object>> updateUserHeartbeat(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "会话ID", required = true)
            @PathVariable("sessionId") String sessionId) {
        
        log.debug("更新用户心跳: userId={}, sessionId={}", userId, sessionId);
        
        boolean success = userOnlineStatusService.updateUserHeartbeat(userId, sessionId);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "sessionId", sessionId,
            "success", success,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 强制用户下线
     */
    @PostMapping("/force-offline/{userId}")
    @Operation(summary = "强制用户下线", description = "管理员强制指定用户下线")
    public ResponseEntity<Map<String, Object>> forceUserOffline(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "会话ID", required = false)
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @Parameter(description = "下线原因", required = true)
            @RequestParam("reason") String reason,
            @Parameter(description = "操作者ID", required = true)
            @RequestParam("operatorId") String operatorId) {
        
        log.info("强制用户下线: userId={}, sessionId={}, reason={}, operator={}", 
                userId, sessionId, reason, operatorId);
        
        boolean success = userOnlineStatusService.forceUserOffline(userId, sessionId, reason, operatorId);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "sessionId", sessionId,
            "reason", reason,
            "operatorId", operatorId,
            "success", success,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 清理过期在线状态
     */
    @PostMapping("/cleanup-expired")
    @Operation(summary = "清理过期在线状态", description = "清理超时未更新心跳的在线状态")
    public ResponseEntity<Map<String, Object>> cleanupExpiredOnlineStatus(
            @Parameter(description = "超时时间（分钟）", required = false)
            @RequestParam(value = "timeoutMinutes", defaultValue = "5") int timeoutMinutes) {
        
        log.info("清理过期在线状态: timeoutMinutes={}", timeoutMinutes);
        
        int cleanedCount = userOnlineStatusService.cleanupExpiredOnlineStatus(timeoutMinutes);
        
        return ResponseEntity.ok(Map.of(
            "timeoutMinutes", timeoutMinutes,
            "cleanedCount", cleanedCount,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 获取用户在线历史
     */
    @GetMapping("/history/{userId}")
    @Operation(summary = "获取用户在线历史", description = "获取指定用户的在线历史记录")
    public ResponseEntity<List<UserStatusResponse.OnlineHistory>> getUserOnlineHistory(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "查询天数", required = false)
            @RequestParam(value = "days", defaultValue = "7") int days) {
        
        log.debug("获取用户在线历史: userId={}, days={}", userId, days);
        
        List<UserStatusResponse.OnlineHistory> history = 
            userOnlineStatusService.getUserOnlineHistory(userId, days);
        
        return ResponseEntity.ok(history);
    }
    
    // ==================== 配置管理 ====================
    
    /**
     * 获取在线状态管理配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取配置信息", description = "获取用户在线状态管理的配置信息")
    public ResponseEntity<Map<String, Object>> getOnlineStatusConfig() {
        
        log.debug("获取在线状态管理配置");
        
        Map<String, Object> config = userOnlineStatusService.getOnlineStatusConfig();
        return ResponseEntity.ok(config);
    }
    
    /**
     * 更新在线状态管理配置
     */
    @PostMapping("/config")
    @Operation(summary = "更新配置信息", description = "动态更新用户在线状态管理的配置")
    public ResponseEntity<Map<String, Object>> updateOnlineStatusConfig(
            @Parameter(description = "配置信息", required = true)
            @RequestBody Map<String, Object> config) {
        
        log.info("更新在线状态管理配置: config={}", config);
        
        boolean success = userOnlineStatusService.updateOnlineStatusConfig(config);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "config", config,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    // ==================== 辅助接口 ====================
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查用户在线状态服务的健康状态")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        
        try {
            // 执行简单的功能检查
            UserStatusResponse.PlatformStats stats = userOnlineStatusService.getPlatformOnlineStats();
            
            return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "user-online-status",
                "totalOnlineUsers", stats.getTotalOnlineUsers(),
                "totalConnections", stats.getTotalConnections(),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("用户在线状态服务健康检查失败: error={}", e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "status", "unhealthy",
                "service", "user-online-status",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
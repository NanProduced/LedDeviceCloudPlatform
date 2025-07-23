package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.message.api.dto.response.UserOnlinePushResult;
import org.nan.cloud.message.service.UserOnlinePushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户在线推送控制器
 * 
 * 提供用户上线后的消息推送功能，包括未读消息推送、任务结果推送等。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user-online-push")
@RequiredArgsConstructor
@Tag(name = "用户在线推送API", description = "用户上线后的消息推送相关接口")
public class UserOnlinePushController {
    
    private final UserOnlinePushService userOnlinePushService;
    
    /**
     * 触发用户在线推送
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 推送结果
     */
    @PostMapping("/trigger/{userId}")
    @Operation(summary = "触发用户在线推送", description = "用户上线后触发消息推送")
    public ResponseEntity<UserOnlinePushResult> triggerUserOnlinePush(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "会话ID", required = false)
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @Parameter(description = "组织ID", required = false)
            @RequestParam(value = "organizationId", required = false) String organizationId) {
        
        log.info("触发用户在线推送 - 用户ID: {}, 会话ID: {}, 组织ID: {}", userId, sessionId, organizationId);
        
        try {
            UserOnlinePushResult result = userOnlinePushService.handleUserOnline(userId, sessionId, organizationId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("触发用户在线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            
            // 返回失败结果
            UserOnlinePushResult result = UserOnlinePushResult.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .organizationId(organizationId)
                    .success(false)
                    .errorMessage("推送失败: " + e.getMessage())
                    .pushedMessageCount(0)
                    .pushedTaskResultCount(0)
                    .failedMessageCount(0)
                    .failedTaskResultCount(0)
                    .status("FAILED")
                    .build();
            
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 获取推送策略
     * 
     * @param organizationId 组织ID
     * @return 推送策略
     */
    @GetMapping("/strategy/{organizationId}")
    @Operation(summary = "获取推送策略", description = "获取指定组织的推送策略配置")
    public ResponseEntity<Map<String, Object>> getPushStrategy(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("organizationId") String organizationId) {
        
        log.info("获取推送策略 - 组织ID: {}", organizationId);
        
        try {
            Map<String, Object> strategy = userOnlinePushService.getPushStrategy(organizationId);
            return ResponseEntity.ok(strategy);
        } catch (Exception e) {
            log.error("获取推送策略失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "organizationId", organizationId,
                "error", e.getMessage(),
                "message", "获取推送策略失败"
            ));
        }
    }
    
    /**
     * 更新推送策略
     * 
     * @param organizationId 组织ID
     * @param strategy 推送策略配置
     * @return 更新结果
     */
    @PostMapping("/strategy/{organizationId}")
    @Operation(summary = "更新推送策略", description = "更新指定组织的推送策略配置")
    public ResponseEntity<Map<String, Object>> updatePushStrategy(
            @Parameter(description = "组织ID", required = true)
            @PathVariable("organizationId") String organizationId,
            @Parameter(description = "推送策略配置", required = true)
            @RequestBody Map<String, Object> strategy) {
        
        log.info("更新推送策略 - 组织ID: {}, 策略: {}", organizationId, strategy);
        
        try {
            boolean success = userOnlinePushService.updatePushStrategy(organizationId, strategy);
            
            return ResponseEntity.ok(Map.of(
                "organizationId", organizationId,
                "success", success,
                "strategy", strategy,
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("更新推送策略失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "organizationId", organizationId,
                "success", false,
                "error", e.getMessage(),
                "message", "更新推送策略失败"
            ));
        }
    }
    
    /**
     * 异步处理用户上线推送
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param organizationId 组织ID
     * @return 异步任务ID
     */
    @PostMapping("/async/{userId}")
    @Operation(summary = "异步处理用户上线推送", description = "异步方式处理用户上线后的消息推送")
    public ResponseEntity<Map<String, Object>> handleUserOnlineAsync(
            @Parameter(description = "用户ID", required = true)
            @PathVariable("userId") String userId,
            @Parameter(description = "会话ID", required = false)
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @Parameter(description = "组织ID", required = false)
            @RequestParam(value = "organizationId", required = false) String organizationId) {
        
        log.info("异步处理用户上线推送 - 用户ID: {}, 会话ID: {}, 组织ID: {}", userId, sessionId, organizationId);
        
        try {
            String taskId = userOnlinePushService.handleUserOnlineAsync(userId, sessionId, organizationId);
            
            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "sessionId", sessionId,
                "organizationId", organizationId,
                "taskId", taskId,
                "message", "异步推送任务已启动",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("异步处理用户上线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                "userId", userId,
                "error", e.getMessage(),
                "message", "异步推送任务启动失败"
            ));
        }
    }
    
    /**
     * 查询异步推送结果
     * 
     * @param taskId 异步任务ID
     * @return 推送结果
     */
    @GetMapping("/async/result/{taskId}")
    @Operation(summary = "查询异步推送结果", description = "查询异步推送任务的执行状态和结果")
    public ResponseEntity<UserOnlinePushResult> getAsyncPushResult(
            @Parameter(description = "异步任务ID", required = true)
            @PathVariable("taskId") String taskId) {
        
        log.debug("查询异步推送结果 - 任务ID: {}", taskId);
        
        try {
            UserOnlinePushResult result = userOnlinePushService.getAsyncPushResult(taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("查询异步推送结果失败: taskId={}, error={}", taskId, e.getMessage(), e);
            
            // 返回错误结果
            UserOnlinePushResult result = UserOnlinePushResult.builder()
                    .asyncTaskId(taskId)
                    .success(false)
                    .errorMessage("查询异步推送结果失败: " + e.getMessage())
                    .pushedMessageCount(0)
                    .pushedTaskResultCount(0)
                    .failedMessageCount(0)
                    .failedTaskResultCount(0)
                    .status("FAILED")
                    .build();
            
            return ResponseEntity.status(500).body(result);
        }
    }
}
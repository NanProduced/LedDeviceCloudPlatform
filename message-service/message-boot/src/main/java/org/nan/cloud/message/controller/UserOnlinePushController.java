package org.nan.cloud.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.web.response.ApiResponse;
import org.nan.cloud.message.api.dto.response.UserOnlinePushResult;
import org.nan.cloud.message.service.UserOnlinePushService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Map;

/**
 * 用户上线推送控制器
 * 
 * 提供用户上线推送功能的REST API接口，支持手动触发推送、查询推送结果、
 * 管理推送策略等功能。主要用于管理员操作和系统集成。
 * 
 * 主要功能：
 * - 手动触发用户上线推送
 * - 异步推送任务管理
 * - 推送结果查询和统计
 * - 推送策略配置管理
 * - 推送状态监控
 * 
 * 权限要求：
 * - 管理员权限：推送策略管理
 * - 用户权限：个人推送触发和查询
 * - 系统权限：批量推送和监控
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户上线推送", description = "用户上线推送管理API")
public class UserOnlinePushController {
    
    private final UserOnlinePushService userOnlinePushService;
    
    // ==================== 推送触发接口 ====================
    
    /**
     * 手动触发用户上线推送
     * 
     * 管理员或系统可以手动为指定用户触发上线推送，
     * 用于测试、故障恢复或特殊业务场景。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 推送结果
     */
    @PostMapping("/trigger")
    @Operation(summary = "手动触发用户上线推送", 
               description = "为指定用户手动触发上线推送，推送所有未读消息和未查看任务结果")
    public ResponseEntity<ApiResponse<UserOnlinePushResult>> triggerUserOnlinePush(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("手动触发用户上线推送: userId={}, sessionId={}, organizationId={}", 
                    userId, sessionId, organizationId);
            
            UserOnlinePushResult result = userOnlinePushService.handleUserOnline(
                userId, sessionId, organizationId);
            
            if (result.getSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(result, "推送成功完成"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("推送部分失败: " + result.getErrorMessage(), result));
            }
            
        } catch (Exception e) {
            log.error("手动触发用户上线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送触发失败: " + e.getMessage()));
        }
    }
    
    /**
     * 异步触发用户上线推送
     * 
     * 异步方式触发推送，适用于推送内容较多或需要避免阻塞的场景。
     * 返回任务ID，可用于查询推送进度和结果。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 异步任务ID
     */
    @PostMapping("/trigger/async")
    @Operation(summary = "异步触发用户上线推送", 
               description = "异步方式触发用户上线推送，返回任务ID用于查询进度")
    public ResponseEntity<ApiResponse<String>> triggerUserOnlinePushAsync(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("异步触发用户上线推送: userId={}, sessionId={}, organizationId={}", 
                    userId, sessionId, organizationId);
            
            String taskId = userOnlinePushService.handleUserOnlineAsync(
                userId, sessionId, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(taskId, "异步推送任务已启动"));
            
        } catch (Exception e) {
            log.error("异步触发用户上线推送失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("异步推送启动失败: " + e.getMessage()));
        }
    }
    
    // ==================== 具体推送类型接口 ====================
    
    /**
     * 推送用户未读消息
     * 
     * 单独推送用户的未读消息，不包括任务结果。
     * 支持限制推送数量，避免消息过载。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @param maxCount 最大推送数量
     * @return 推送的消息数量
     */
    @PostMapping("/messages")
    @Operation(summary = "推送用户未读消息", 
               description = "单独推送用户的未读消息，支持限制推送数量")
    public ResponseEntity<ApiResponse<Integer>> pushUnreadMessages(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId,
            
            @Parameter(description = "最大推送数量，范围1-50")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int maxCount) {
        
        try {
            log.info("推送用户未读消息: userId={}, sessionId={}, maxCount={}", 
                    userId, sessionId, maxCount);
            
            int pushedCount = userOnlinePushService.pushUnreadMessages(
                userId, sessionId, organizationId, maxCount);
            
            return ResponseEntity.ok(ApiResponse.success(pushedCount, 
                String.format("成功推送%d条未读消息", pushedCount)));
            
        } catch (Exception e) {
            log.error("推送用户未读消息失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送未读消息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 推送用户未查看任务结果
     * 
     * 单独推送用户的未查看任务结果，不包括普通消息。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 推送的任务结果数量
     */
    @PostMapping("/tasks")
    @Operation(summary = "推送用户未查看任务结果", 
               description = "单独推送用户的未查看任务结果")
    public ResponseEntity<ApiResponse<Integer>> pushUnviewedTaskResults(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("推送用户未查看任务结果: userId={}, sessionId={}", userId, sessionId);
            
            int pushedCount = userOnlinePushService.pushUnviewedTaskResults(
                userId, sessionId, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(pushedCount, 
                String.format("成功推送%d个未查看任务结果", pushedCount)));
            
        } catch (Exception e) {
            log.error("推送用户未查看任务结果失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送任务结果失败: " + e.getMessage()));
        }
    }
    
    /**
     * 推送高优先级消息
     * 
     * 仅推送高优先级或紧急消息，用于重要通知的快速推送。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 推送的高优先级消息数量
     */
    @PostMapping("/priority-messages")
    @Operation(summary = "推送高优先级消息", 
               description = "仅推送高优先级或紧急消息")
    public ResponseEntity<ApiResponse<Integer>> pushHighPriorityMessages(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("推送用户高优先级消息: userId={}, sessionId={}", userId, sessionId);
            
            int pushedCount = userOnlinePushService.pushHighPriorityMessages(
                userId, sessionId, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(pushedCount, 
                String.format("成功推送%d条高优先级消息", pushedCount)));
            
        } catch (Exception e) {
            log.error("推送用户高优先级消息失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送高优先级消息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 推送用户统计信息
     * 
     * 推送用户的统计信息，如未读消息数、在线状态等。
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID（可选）
     * @param organizationId 组织ID
     * @return 是否推送成功
     */
    @PostMapping("/statistics")
    @Operation(summary = "推送用户统计信息", 
               description = "推送用户的统计信息，如未读消息数、在线状态等")
    public ResponseEntity<ApiResponse<Boolean>> pushUserStatistics(
            @Parameter(description = "用户ID", required = true)
            @RequestParam @NotBlank String userId,
            
            @Parameter(description = "会话ID，用于会话级推送")
            @RequestParam(required = false) String sessionId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.info("推送用户统计信息: userId={}, sessionId={}", userId, sessionId);
            
            boolean success = userOnlinePushService.pushUserStatistics(
                userId, sessionId, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(success, 
                success ? "统计信息推送成功" : "统计信息推送失败"));
            
        } catch (Exception e) {
            log.error("推送用户统计信息失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("推送统计信息失败: " + e.getMessage()));
        }
    }
    
    // ==================== 查询和监控接口 ====================
    
    /**
     * 查询异步推送结果
     * 
     * 根据任务ID查询异步推送的执行状态和结果。
     * 
     * @param taskId 异步任务ID
     * @return 推送结果
     */
    @GetMapping("/async/{taskId}")
    @Operation(summary = "查询异步推送结果", 
               description = "根据任务ID查询异步推送的执行状态和结果")
    public ResponseEntity<ApiResponse<UserOnlinePushResult>> getAsyncPushResult(
            @Parameter(description = "异步任务ID", required = true)
            @PathVariable @NotBlank String taskId) {
        
        try {
            log.debug("查询异步推送结果: taskId={}", taskId);
            
            UserOnlinePushResult result = userOnlinePushService.getAsyncPushResult(taskId);
            
            if (result != null) {
                return ResponseEntity.ok(ApiResponse.success(result, "查询成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("未找到指定的异步任务"));
            }
            
        } catch (Exception e) {
            log.error("查询异步推送结果失败: taskId={}, error={}", taskId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("查询异步推送结果失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查用户是否需要推送
     * 
     * 检查指定用户当前是否有需要推送的内容。
     * 
     * @param userId 用户ID
     * @param organizationId 组织ID
     * @return 是否需要推送
     */
    @GetMapping("/check/{userId}")
    @Operation(summary = "检查用户是否需要推送", 
               description = "检查指定用户当前是否有需要推送的内容")
    public ResponseEntity<ApiResponse<Boolean>> shouldPushToUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable @NotBlank String userId,
            
            @Parameter(description = "组织ID", required = true)
            @RequestParam @NotBlank String organizationId) {
        
        try {
            log.debug("检查用户推送需求: userId={}, organizationId={}", userId, organizationId);
            
            boolean shouldPush = userOnlinePushService.shouldPushToUser(userId, organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(shouldPush, 
                shouldPush ? "用户需要推送" : "用户暂无需要推送的内容"));
            
        } catch (Exception e) {
            log.error("检查用户推送需求失败: userId={}, error={}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("检查推送需求失败: " + e.getMessage()));
        }
    }
    
    // ==================== 推送策略管理接口 ====================
    
    /**
     * 获取推送策略配置
     * 
     * 获取指定组织的推送策略配置信息。
     * 
     * @param organizationId 组织ID
     * @return 推送策略配置
     */
    @GetMapping("/strategy/{organizationId}")
    @Operation(summary = "获取推送策略配置", 
               description = "获取指定组织的推送策略配置信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPushStrategy(
            @Parameter(description = "组织ID", required = true)
            @PathVariable @NotBlank String organizationId) {
        
        try {
            log.debug("获取推送策略: organizationId={}", organizationId);
            
            Map<String, Object> strategy = userOnlinePushService.getPushStrategy(organizationId);
            
            return ResponseEntity.ok(ApiResponse.success(strategy, "获取推送策略成功"));
            
        } catch (Exception e) {
            log.error("获取推送策略失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("获取推送策略失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新推送策略配置
     * 
     * 更新指定组织的推送策略配置。
     * 
     * @param organizationId 组织ID
     * @param strategy 新的推送策略配置
     * @return 是否更新成功
     */
    @PutMapping("/strategy/{organizationId}")
    @Operation(summary = "更新推送策略配置", 
               description = "更新指定组织的推送策略配置")
    public ResponseEntity<ApiResponse<Boolean>> updatePushStrategy(
            @Parameter(description = "组织ID", required = true)
            @PathVariable @NotBlank String organizationId,
            
            @Parameter(description = "推送策略配置", required = true)
            @RequestBody Map<String, Object> strategy) {
        
        try {
            log.info("更新推送策略: organizationId={}, strategy={}", organizationId, strategy);
            
            boolean success = userOnlinePushService.updatePushStrategy(organizationId, strategy);
            
            return ResponseEntity.ok(ApiResponse.success(success, 
                success ? "推送策略更新成功" : "推送策略更新失败"));
            
        } catch (Exception e) {
            log.error("更新推送策略失败: organizationId={}, error={}", organizationId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("更新推送策略失败: " + e.getMessage()));
        }
    }
}
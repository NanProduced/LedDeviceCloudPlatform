package org.nan.cloud.terminal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.api.dto.command.CommandConfirmDTO;
import org.nan.cloud.terminal.api.dto.command.CommandRequestDTO;
import org.nan.cloud.terminal.api.dto.command.CommandResponseDTO;
import org.nan.cloud.terminal.api.dto.status.DeviceStatusDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 设备核心API控制器
 * 
 * 实现WordPress REST API兼容的设备接口：
 * 1. GET /wp-json/wp/v2/comments - 设备获取待执行指令
 * 2. POST /wp-json/wp/v2/comments - 设备确认指令执行结果
 * 3. PUT /wp-json/screen/v1/status - 设备状态上报
 * 
 * 设计原则：
 * - 兼容WordPress REST API格式，便于现有设备接入
 * - 支持Basic Auth认证，适合设备端实现
 * - 高性能Redis缓存，减少数据库查询压力
 * - 详细的操作日志，便于问题排查
 * - 优雅的错误处理，提供友好的错误信息
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class DeviceApiController {

    private final StringRedisTemplate redisTemplate;

    /**
     * 设备获取待执行指令
     * 
     * GET /wp-json/wp/v2/comments?clt_type=terminal&device_id=xxx
     * 
     * 兼容WordPress comments接口格式，返回设备待执行的指令列表
     */
    @GetMapping("/wp-json/wp/v2/comments")
    public ResponseEntity<CommandResponseDTO> getDeviceCommands(
            @Valid CommandRequestDTO request,
            Authentication authentication) {
        
        try {
            String deviceId = extractDeviceId(authentication, request.getDeviceId());
            
            log.info("设备请求指令: deviceId={}, lastCommandId={}, maxCommands={}", 
                deviceId, request.getLastCommandId(), request.getMaxCommands());

            // 检查设备认证状态
            if (!isDeviceAuthenticated(deviceId)) {
                log.warn("设备未认证: deviceId={}", deviceId);
                return ResponseEntity.status(401).body(
                    CommandResponseDTO.builder()
                        .commands(new ArrayList<>())
                        .total(0)
                        .hasMore(false)
                        .build()
                );
            }

            // 从Redis获取设备待执行指令
            String commandQueueKey = "terminal:command:queue:" + deviceId;
            String commandListJson = redisTemplate.opsForValue().get(commandQueueKey);
            
            // 暂时返回空指令列表（后续实现具体的指令管理逻辑）
            CommandResponseDTO response = CommandResponseDTO.builder()
                .commands(new ArrayList<>())
                .total(0)
                .hasMore(false)
                .build();

            // 记录设备指令查询日志
            String logKey = "terminal:log:command_query:" + deviceId + ":" + System.currentTimeMillis();
            Map<String, Object> logData = new HashMap<>();
            logData.put("deviceId", deviceId);
            logData.put("requestTime", LocalDateTime.now().toString());
            logData.put("lastCommandId", request.getLastCommandId());
            logData.put("maxCommands", request.getMaxCommands());
            logData.put("commandCount", response.getTotal());
            
            // 异步记录日志到Redis（1小时TTL）
            try {
                redisTemplate.opsForValue().set(logKey, logData.toString(), 1, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("记录指令查询日志失败: deviceId={}", deviceId, e);
            }

            log.debug("设备指令查询成功: deviceId={}, 返回指令数={}", deviceId, response.getTotal());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("设备指令查询异常", e);
            return ResponseEntity.status(500).body(
                CommandResponseDTO.builder()
                    .commands(new ArrayList<>())
                    .total(0)
                    .hasMore(false)
                    .build()
            );
        }
    }

    /**
     * 设备确认指令执行结果
     * 
     * POST /wp-json/wp/v2/comments
     * 
     * 兼容WordPress创建comment的接口格式，用于设备上报指令执行结果
     */
    @PostMapping("/wp-json/wp/v2/comments")
    public ResponseEntity<Map<String, Object>> confirmCommandExecution(
            @Valid @RequestBody CommandConfirmDTO confirmDTO,
            Authentication authentication) {
        
        try {
            String deviceId = extractDeviceId(authentication, confirmDTO.getDeviceId());
            
            log.info("设备确认指令执行: deviceId={}, commandId={}, status={}, duration={}ms", 
                deviceId, confirmDTO.getCommandId(), confirmDTO.getStatus(), 
                confirmDTO.getExecutionDurationMs());

            // 检查设备认证状态
            if (!isDeviceAuthenticated(deviceId)) {
                log.warn("设备未认证: deviceId={}", deviceId);
                return ResponseEntity.status(401).body(createErrorResponse("设备未认证"));
            }

            // 验证指令ID是否存在
            String commandKey = "terminal:command:status:" + confirmDTO.getCommandId();
            if (!Boolean.TRUE.equals(redisTemplate.hasKey(commandKey))) {
                log.warn("指令不存在: deviceId={}, commandId={}", deviceId, confirmDTO.getCommandId());
                return ResponseEntity.status(404).body(createErrorResponse("指令不存在"));
            }

            // 更新指令执行状态
            Map<String, Object> commandStatus = new HashMap<>();
            commandStatus.put("commandId", confirmDTO.getCommandId());
            commandStatus.put("deviceId", deviceId);
            commandStatus.put("status", confirmDTO.getStatus());
            commandStatus.put("content", confirmDTO.getContent());
            commandStatus.put("executionTime", confirmDTO.getExecutionTime() != null ? 
                confirmDTO.getExecutionTime().toString() : LocalDateTime.now().toString());
            commandStatus.put("executionDurationMs", confirmDTO.getExecutionDurationMs());
            commandStatus.put("resultData", confirmDTO.getResultData());
            commandStatus.put("updatedAt", LocalDateTime.now().toString());

            // 保存到Redis（24小时TTL）
            redisTemplate.opsForValue().set(commandKey, commandStatus.toString(), 24, TimeUnit.HOURS);

            // 处理执行结果
            if (confirmDTO.isSuccess()) {
                // 执行成功，移除队列中的指令
                String queueKey = "terminal:command:queue:" + deviceId;
                // TODO: 实现指令队列管理逻辑
                
                log.info("指令执行成功: deviceId={}, commandId={}", deviceId, confirmDTO.getCommandId());
                
            } else if (confirmDTO.shouldRetry()) {
                // 需要重试，更新重试计数
                // TODO: 实现重试逻辑
                
                log.warn("指令执行失败，需要重试: deviceId={}, commandId={}, status={}, error={}", 
                    deviceId, confirmDTO.getCommandId(), confirmDTO.getStatus(), confirmDTO.getErrorMessage());
                
            } else {
                // 失败且不重试，标记为最终失败
                // TODO: 实现失败处理逻辑
                
                log.error("指令执行最终失败: deviceId={}, commandId={}, status={}, error={}", 
                    deviceId, confirmDTO.getCommandId(), confirmDTO.getStatus(), confirmDTO.getErrorMessage());
            }

            // 返回WordPress格式的响应
            Map<String, Object> response = new HashMap<>();
            response.put("id", confirmDTO.getCommandId());
            response.put("status", confirmDTO.getStatus() >= 200 && confirmDTO.getStatus() < 300 ? "approved" : "hold");
            response.put("date", LocalDateTime.now().toString());
            response.put("content", Map.of("rendered", confirmDTO.getContent()));
            response.put("author", deviceId);
            response.put("meta", Map.of(
                "execution_status", confirmDTO.getStatus(),
                "execution_duration_ms", confirmDTO.getExecutionDurationMs(),
                "device_status", confirmDTO.getDeviceStatus()
            ));

            return ResponseEntity.status(201).body(response);

        } catch (Exception e) {
            log.error("指令确认处理异常", e);
            return ResponseEntity.status(500).body(createErrorResponse("服务器内部错误"));
        }
    }

    /**
     * 设备状态上报
     * 
     * PUT /wp-json/screen/v1/status
     * 
     * 设备定时上报运行状态，用于监控和管理
     */
    @PutMapping("/wp-json/screen/v1/status")
    public ResponseEntity<Map<String, Object>> updateDeviceStatus(
            @Valid @RequestBody DeviceStatusDTO statusDTO,
            Authentication authentication) {
        
        try {
            String deviceId = extractDeviceId(authentication, statusDTO.getDeviceId());
            
            log.info("设备状态上报: deviceId={}, status={}, timestamp={}", 
                deviceId, statusDTO.getStatus(), statusDTO.getTimestamp());

            // 检查设备认证状态
            if (!isDeviceAuthenticated(deviceId)) {
                log.warn("设备未认证: deviceId={}", deviceId);
                return ResponseEntity.status(401).body(createErrorResponse("设备未认证"));
            }

            // 保存设备状态到Redis
            String statusKey = "terminal:device:status:" + deviceId;
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("deviceId", deviceId);
            statusData.put("status", statusDTO.getStatus());
            statusData.put("timestamp", statusDTO.getTimestamp().toString());
            statusData.put("systemInfo", statusDTO.getSystemInfo());
            statusData.put("networkInfo", statusDTO.getNetworkInfo());
            statusData.put("applicationInfo", statusDTO.getApplicationInfo());
            statusData.put("hardwareInfo", statusDTO.getHardwareInfo());
            statusData.put("errors", statusDTO.getErrors());
            statusData.put("alerts", statusDTO.getAlerts());
            statusData.put("updatedAt", LocalDateTime.now().toString());

            // 保存状态（1小时TTL，定期更新）
            redisTemplate.opsForValue().set(statusKey, statusData.toString(), 1, TimeUnit.HOURS);

            // 更新设备在线状态
            String onlineKey = "terminal:device:online:" + deviceId;
            redisTemplate.opsForValue().set(onlineKey, String.valueOf(System.currentTimeMillis()), 
                65, TimeUnit.SECONDS);

            // 处理告警信息
            if (statusDTO.getAlerts() != null && !statusDTO.getAlerts().isEmpty()) {
                handleDeviceAlerts(deviceId, statusDTO.getAlerts());
            }

            // 处理错误信息
            if (statusDTO.getErrors() != null && !statusDTO.getErrors().isEmpty()) {
                handleDeviceErrors(deviceId, statusDTO.getErrors());
            }

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("device_id", deviceId);
            response.put("status", statusDTO.getStatus());
            response.put("received_at", LocalDateTime.now().toString());
            response.put("message", "设备状态更新成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("设备状态上报处理异常", e);
            return ResponseEntity.status(500).body(createErrorResponse("服务器内部错误"));
        }
    }

    /**
     * 从认证信息中提取设备ID
     */
    private String extractDeviceId(Authentication authentication, String requestDeviceId) {
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        return requestDeviceId;
    }

    /**
     * 检查设备认证状态
     */
    private boolean isDeviceAuthenticated(String deviceId) {
        if (deviceId == null) {
            return false;
        }
        String authKey = "terminal:auth:success:" + deviceId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(authKey));
    }

    /**
     * 处理设备告警
     */
    private void handleDeviceAlerts(String deviceId, java.util.List<DeviceStatusDTO.AlertInfo> alerts) {
        try {
            for (DeviceStatusDTO.AlertInfo alert : alerts) {
                String alertKey = "terminal:alert:" + deviceId + ":" + alert.getAlertType() + ":" + 
                    System.currentTimeMillis();
                
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("deviceId", deviceId);
                alertData.put("alertType", alert.getAlertType());
                alertData.put("alertMessage", alert.getAlertMessage());
                alertData.put("alertLevel", alert.getAlertLevel());
                alertData.put("triggeredAt", alert.getTriggeredAt().toString());
                alertData.put("threshold", alert.getThreshold());
                alertData.put("currentValue", alert.getCurrentValue());
                
                // 保存告警信息（24小时TTL）
                redisTemplate.opsForValue().set(alertKey, alertData.toString(), 24, TimeUnit.HOURS);
                
                log.warn("设备告警: deviceId={}, type={}, level={}, message={}", 
                    deviceId, alert.getAlertType(), alert.getAlertLevel(), alert.getAlertMessage());
            }
        } catch (Exception e) {
            log.error("处理设备告警异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 处理设备错误
     */
    private void handleDeviceErrors(String deviceId, java.util.List<DeviceStatusDTO.ErrorInfo> errors) {
        try {
            for (DeviceStatusDTO.ErrorInfo error : errors) {
                String errorKey = "terminal:error:" + deviceId + ":" + error.getErrorCode() + ":" + 
                    System.currentTimeMillis();
                
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("deviceId", deviceId);
                errorData.put("errorCode", error.getErrorCode());
                errorData.put("errorMessage", error.getErrorMessage());
                errorData.put("errorLevel", error.getErrorLevel());
                errorData.put("occurredAt", error.getOccurredAt().toString());
                errorData.put("occurrenceCount", error.getOccurrenceCount());
                
                // 保存错误信息（24小时TTL）
                redisTemplate.opsForValue().set(errorKey, errorData.toString(), 24, TimeUnit.HOURS);
                
                log.error("设备错误: deviceId={}, code={}, level={}, message={}, count={}", 
                    deviceId, error.getErrorCode(), error.getErrorLevel(), 
                    error.getErrorMessage(), error.getOccurrenceCount());
            }
        } catch (Exception e) {
            log.error("处理设备错误异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now().toString());
        return error;
    }
}
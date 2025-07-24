package org.nan.cloud.terminal.api.dto.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指令确认DTO
 * 
 * 设备执行指令后向服务端确认执行结果，对应WordPress REST API格式：
 * POST /wp-json/wp/v2/comments
 * 
 * 请求体格式（兼容WordPress comment创建）：
 * {
 *   "post": 0,
 *   "parent": 0,
 *   "content": "execution result",
 *   "status": 200,
 *   "command_id": "cmd_001",
 *   "device_id": "device_001",
 *   "execution_time": "2024-01-15T10:35:00",
 *   "result_data": { ... }
 * }
 * 
 * 状态码规范：
 * - 200-299: 执行成功，允许继续执行后续指令
 * - 300-399: 部分成功，需要重试或调整参数
 * - 400-499: 客户端错误，指令格式或参数错误
 * - 500-599: 服务端错误，设备内部错误
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
public class CommandConfirmDTO {

    /**
     * 文章ID（兼容WordPress，固定为0）
     */
    @JsonProperty("post")
    private Integer post = 0;

    /**
     * 父评论ID（兼容WordPress，固定为0）
     */
    @JsonProperty("parent")
    private Integer parent = 0;

    /**
     * 执行结果内容（兼容WordPress comment content）
     */
    @JsonProperty("content")
    @NotBlank(message = "执行结果内容不能为空")
    private String content;

    /**
     * 执行状态码
     * 200-299: 成功
     * 300-399: 部分成功/重定向
     * 400-499: 客户端错误
     * 500-599: 服务端错误
     */
    @JsonProperty("status")
    @NotNull(message = "执行状态不能为空")
    @Min(value = 200, message = "状态码不能小于200")
    @Max(value = 599, message = "状态码不能大于599")
    private Integer status;

    /**
     * 指令ID
     */
    @JsonProperty("command_id")
    @NotBlank(message = "指令ID不能为空")
    private String commandId;

    /**
     * 设备ID
     */
    @JsonProperty("device_id")
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;

    /**
     * 指令执行时间
     */
    @JsonProperty("execution_time")
    private LocalDateTime executionTime;

    /**
     * 执行耗时（毫秒）
     */
    @JsonProperty("execution_duration_ms")
    private Long executionDurationMs;

    /**
     * 执行结果数据
     */
    @JsonProperty("result_data")
    private Map<String, Object> resultData;

    /**
     * 错误信息（当status >= 400时）
     */
    @JsonProperty("error_message")
    private String errorMessage;

    /**
     * 错误代码（当status >= 400时）
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * 设备当前状态
     */
    @JsonProperty("device_status")
    private String deviceStatus;

    /**
     * 资源使用情况
     */
    @JsonProperty("resource_usage")
    private ResourceUsage resourceUsage;

    /**
     * 资源使用情况
     */
    @Data
    public static class ResourceUsage {
        
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
         * 存储使用率（百分比）
         */
        @JsonProperty("storage_usage")
        private Double storageUsage;

        /**
         * 网络使用情况
         */
        @JsonProperty("network_usage")
        private String networkUsage;

        /**
         * 温度（摄氏度）
         */
        @JsonProperty("temperature")
        private Double temperature;
    }

    /**
     * 检查执行是否成功
     */
    public boolean isSuccess() {
        return status != null && status >= 200 && status < 300;
    }

    /**
     * 检查是否需要重试
     */
    public boolean shouldRetry() {
        return status != null && status >= 500 && status < 600;
    }

    /**
     * 检查是否为客户端错误
     */
    public boolean isClientError() {
        return status != null && status >= 400 && status < 500;
    }
}
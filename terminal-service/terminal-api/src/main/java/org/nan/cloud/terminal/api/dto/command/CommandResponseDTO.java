package org.nan.cloud.terminal.api.dto.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备指令响应DTO
 * 
 * 服务端返回给设备的指令列表，兼容WordPress REST API格式：
 * 模拟WordPress comments接口的数据结构
 * 
 * 响应格式：
 * {
 *   "commands": [
 *     {
 *       "id": "cmd_001",
 *       "date": "2024-01-15T10:30:00",
 *       "content": { ... },
 *       "status": "pending",
 *       "priority": "high"
 *     }
 *   ],
 *   "total": 1,
 *   "has_more": false
 * }
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Data
@Builder
public class CommandResponseDTO {

    /**
     * 指令列表
     */
    @JsonProperty("commands")
    private List<CommandItem> commands;

    /**
     * 总指令数量
     */
    @JsonProperty("total")
    private Integer total;

    /**
     * 是否有更多指令
     */
    @JsonProperty("has_more")
    private Boolean hasMore;

    /**
     * 单个指令项
     */
    @Data
    @Builder
    public static class CommandItem {

        /**
         * 指令ID（兼容comment ID）
         */
        @JsonProperty("id")
        private String id;

        /**
         * 指令创建时间（兼容comment date）
         */
        @JsonProperty("date")
        private LocalDateTime date;

        /**
         * 指令内容（兼容comment content）
         */
        @JsonProperty("content")
        private Map<String, Object> content;

        /**
         * 指令状态
         */
        @JsonProperty("status")
        private String status;

        /**
         * 指令优先级
         */
        @JsonProperty("priority")
        private String priority;

        /**
         * 指令类型
         */
        @JsonProperty("type")
        private String type;

        /**
         * 过期时间
         */
        @JsonProperty("expires_at")
        private LocalDateTime expiresAt;

        /**
         * 重试次数
         */
        @JsonProperty("retry_count")
        private Integer retryCount;

        /**
         * 最大重试次数
         */
        @JsonProperty("max_retries")
        private Integer maxRetries;

        /**
         * 目标设备ID
         */
        @JsonProperty("target_device")
        private String targetDevice;

        /**
         * 创建者信息
         */
        @JsonProperty("author")
        private String author;

        /**
         * 元数据
         */
        @JsonProperty("meta")
        private Map<String, Object> meta;
    }
}
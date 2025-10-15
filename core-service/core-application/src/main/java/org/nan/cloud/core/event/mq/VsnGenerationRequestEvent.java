package org.nan.cloud.core.event.mq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * VSN生成请求事件
 * 
 * 用于core-service向file-service发送VSN生成请求
 * 触发异步VSN XML生成和缩略图生成
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VsnGenerationRequestEvent {

    /**
     * 事件类型
     * GENERATE: 生成VSN
     * REGENERATE: 重新生成VSN
     */
    private String eventType;

    /**
     * 节目ID
     */
    private Long programId;

    /**
     * 节目版本号
     */
    private Integer version;

    /**
     * 组织ID
     */
    private Long organizationId;

    /**
     * 用户组ID
     */
    private Long userGroupId;

    /**
     * 创建者用户ID
     */
    private Long userId;

    /**
     * 节目名称
     */
    private String programName;

    /**
     * 节目宽度
     */
    private Integer width;

    /**
     * 节目高度
     */
    private Integer height;

    /**
     * MongoDB内容文档ID
     */
    private String contentId;

    /**
     * 请求优先级
     * HIGH: 高优先级（用户手动触发）
     * NORMAL: 普通优先级（系统自动触发）
     */
    private String priority;

    /**
     * 事件时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 扩展属性
     */
    private Map<String, Object> extras;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        GENERATE("GENERATE"),
        REGENERATE("REGENERATE");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 优先级枚举
     */
    public enum Priority {
        HIGH("HIGH"),
        NORMAL("NORMAL");

        private final String value;

        Priority(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
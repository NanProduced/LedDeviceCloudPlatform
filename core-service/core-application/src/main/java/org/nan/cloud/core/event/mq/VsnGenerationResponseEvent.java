package org.nan.cloud.core.event.mq;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nan.cloud.program.enums.VsnGenerationStatusEnum;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * VSN生成响应事件
 * 
 * 用于file-service向core-service返回VSN生成结果
 * 包含生成状态、文件路径、错误信息等
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VsnGenerationResponseEvent {

    /**
     * 事件类型
     * COMPLETED: 生成完成
     * FAILED: 生成失败
     * PROGRESS: 生成进度（可选）
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
     * VSN生成状态
     */
    private VsnGenerationStatusEnum status;

    /**
     * VSN文件ID
     */
    private String vsnFileId;

    /**
     * VSN文件路径
     */
    private String vsnFilePath;

    /**
     * VSN文件访问URL
     */
    private String vsnFileUrl;

    /**
     * VSN文件大小（字节）
     */
    private Long vsnFileSize;

    /**
     * 缩略图路径
     */
    private String thumbnailPath;

    /**
     * 缩略图访问URL
     */
    private String thumbnailUrl;

    /**
     * 处理开始时间
     */
    private LocalDateTime startTime;

    /**
     * 处理结束时间
     */
    private LocalDateTime endTime;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误详情
     */
    private String errorDetails;

    /**
     * 生成进度（0-100）
     */
    private Integer progress;

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
        COMPLETED("COMPLETED"),
        FAILED("FAILED"),
        PROGRESS("PROGRESS");

        private final String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
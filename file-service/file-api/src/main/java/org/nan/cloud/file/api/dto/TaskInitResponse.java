package org.nan.cloud.file.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异步任务初始化响应
 * 
 * 用于立即返回任务ID，让前端可以订阅任务进度
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskInitResponse {

    /**
     * 任务ID - 前端用于追踪任务状态
     */
    private String taskId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 文件名称
     */
    private String filename;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 组织ID
     */
    private String organizationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 预估处理时长（用于前端展示）
     */
    private String estimatedDuration;

    /**
     * 任务创建时间
     */
    private LocalDateTime createTime;

    /**
     * 进度订阅URL（WebSocket）
     */
    private String progressSubscriptionUrl;

    /**
     * 附加信息
     */
    private String message;
}
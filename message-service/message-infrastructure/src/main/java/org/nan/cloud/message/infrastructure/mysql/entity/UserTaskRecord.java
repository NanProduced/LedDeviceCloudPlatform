package org.nan.cloud.message.infrastructure.mysql.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户任务记录实体类
 * 
 * 记录用户发起的异步任务信息，用于任务结果推送和历史查询。
 * 解决用户退出登录后任务结果丢失的问题，支持多重通知机制。
 * 
 * 业务场景：
 * - 数据导出任务跟踪
 * - 报告生成任务管理
 * - 批量设备操作记录
 * - 长时间运行任务监控
 * - 任务结果历史查询
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_task_record")
public class UserTaskRecord {
    
    /**
     * 主键ID - 自增长
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 任务唯一标识
     * 格式：task-{taskType}-{timestamp}-{random}
     */
    @TableField("task_id")
    private String taskId;
    
    /**
     * 用户ID
     * 发起任务的用户
     */
    @TableField("user_id")
    private String userId;
    
    /**
     * 会话ID
     * 发起任务时的WebSocket会话ID，用于精确推送结果
     * 可能为空（用户已退出登录）
     */
    @TableField("session_id")
    private String sessionId;
    
    /**
     * 组织ID
     * 用于多租户数据隔离
     */
    @TableField("organization_id")
    private String organizationId;
    
    /**
     * 任务类型
     * 
     * 枚举值：
     * - DATA_EXPORT: 数据导出
     * - REPORT_GENERATE: 报告生成
     * - BATCH_DEVICE_CONTROL: 批量设备控制
     * - DEVICE_CONFIG_UPDATE: 设备配置更新
     * - SYSTEM_BACKUP: 系统备份
     */
    @TableField("task_type")
    private String taskType;
    
    /**
     * 任务名称
     * 用户友好的任务描述
     */
    @TableField("task_name")
    private String taskName;
    
    /**
     * 任务状态
     * 
     * 枚举值：
     * - PENDING: 等待执行
     * - PROCESSING: 执行中
     * - SUCCESS: 执行成功
     * - FAILED: 执行失败
     * - CANCELLED: 已取消
     */
    @TableField("status")
    private String status;
    
    /**
     * 任务进度
     * 百分比值 0-100
     */
    @TableField("progress")
    private Integer progress;
    
    /**
     * 错误消息
     * 任务失败时的错误描述
     */
    @TableField("error_message")
    private String errorMessage;
    
    /**
     * 任务创建时间
     * 用户发起任务的时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 任务开始时间
     * 任务实际开始执行的时间
     */
    @TableField("started_time")
    private LocalDateTime startedTime;
    
    /**
     * 任务完成时间
     * 任务执行结束的时间（成功或失败）
     */
    @TableField("completed_time")
    private LocalDateTime completedTime;
    
    /**
     * 结果查看标记
     * 标识用户是否已查看任务结果
     */
    @TableField("is_result_viewed")
    private Boolean isResultViewed;
    
    /**
     * 结果通知状态
     * 记录结果通知的发送状态
     * 
     * 枚举值：
     * - NOT_SENT: 未发送
     * - SENT_TO_SESSION: 已发送到原始会话
     * - SENT_TO_USER: 已发送到用户其他会话
     * - SENT_TO_QUEUE: 已发送到消息队列
     */
    @TableField("notification_status")
    private String notificationStatus;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
package org.nan.cloud.message.infrastructure.mysql.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息基础信息实体类
 * 
 * 存储消息的元数据信息，包括消息ID、类型、状态、时间戳等关键信息。
 * 与MongoDB中的消息详细内容形成关联，通过messageId进行关联查询。
 * 
 * 业务场景：
 * - 消息状态管理和查询
 * - 消息统计分析
 * - 快速过滤和分页查询
 * - 消息生命周期跟踪
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message_info")
public class MessageInfo {
    
    /**
     * 主键ID - 自增长
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 消息唯一标识
     * 格式：msg-{timestamp}-{random}
     * 与MongoDB中的messageId字段对应
     */
    @TableField("message_id")
    private String messageId;
    
    /**
     * 消息类型
     * 
     * 枚举值：
     * - SYSTEM_NOTIFICATION: 系统通知
     * - USER_MESSAGE: 用户消息  
     * - DEVICE_ALERT: 设备告警
     * - TASK_RESULT: 任务结果
     * - BROADCAST: 广播消息
     */
    @TableField("message_type")
    private String messageType;
    
    /**
     * 发送者用户ID
     * 系统消息时为空
     */
    @TableField("sender_id")
    private String senderId;
    
    /**
     * 接收者用户ID
     * 广播消息时为空
     */
    @TableField("receiver_id")
    private String receiverId;
    
    /**
     * 组织ID
     * 用于多租户数据隔离
     */
    @TableField("organization_id")
    private String organizationId;
    
    /**
     * 消息标题
     */
    @TableField("title")
    private String title;
    
    /**
     * 消息优先级
     * 1-低优先级 2-普通优先级 3-高优先级 4-紧急优先级
     */
    @TableField("priority")
    private Integer priority;
    
    /**
     * 消息状态
     * 
     * 枚举值：
     * - PENDING: 待处理
     * - SENT: 已发送
     * - DELIVERED: 已送达
     * - READ: 已读
     * - FAILED: 发送失败
     */
    @TableField("status")
    private String status;
    
    /**
     * 创建时间
     * 消息生成时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 发送时间
     * 实际发送到客户端的时间
     */
    @TableField("sent_time")
    private LocalDateTime sentTime;
    
    /**
     * 送达时间
     * 客户端确认收到消息的时间
     */
    @TableField("delivered_time")
    private LocalDateTime deliveredTime;
    
    /**
     * 已读时间
     * 用户查看消息的时间
     */
    @TableField("read_time")
    private LocalDateTime readTime;
    
    /**
     * 过期时间
     * 消息自动失效时间，过期后不再推送
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * 更新时间
     * 记录状态变更时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
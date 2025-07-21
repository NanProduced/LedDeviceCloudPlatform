package org.nan.cloud.message.infrastructure.mysql.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息模板实体类
 * 
 * 存储消息模板的基础信息和元数据，详细的模板内容存储在MongoDB中。
 * 支持消息模板的统一管理、版本控制和快速查询。
 * 
 * 业务场景：
 * - 设备告警消息模板
 * - 系统通知消息模板
 * - 任务完成通知模板
 * - 自定义业务消息模板
 * - 模板的启用/禁用管理
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message_template")
public class MessageTemplate {
    
    /**
     * 主键ID - 自增长
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    /**
     * 模板唯一标识
     * 格式：tpl-{templateType}-{organizationId}-{version}
     */
    @TableField("template_id")
    private String templateId;
    
    /**
     * 模板名称
     * 用户友好的模板名称
     */
    @TableField("template_name")
    private String templateName;
    
    /**
     * 模板类型
     * 
     * 枚举值：
     * - DEVICE_ALERT: 设备告警模板
     * - SYSTEM_NOTIFICATION: 系统通知模板
     * - TASK_COMPLETION: 任务完成通知模板
     * - USER_WELCOME: 用户欢迎消息模板
     * - MAINTENANCE_NOTICE: 维护通知模板
     * - CUSTOM: 自定义模板
     */
    @TableField("template_type")
    private String templateType;
    
    /**
     * 组织ID
     * 用于多租户模板管理，不同组织可以有不同的模板
     */
    @TableField("organization_id")
    private String organizationId;
    
    /**
     * 模板类别
     * 用于模板分组管理
     * 
     * 枚举值：
     * - ALERT: 告警类
     * - NOTIFICATION: 通知类
     * - MARKETING: 营销类
     * - OPERATIONAL: 运营类
     */
    @TableField("template_category")
    private String templateCategory;
    
    /**
     * 标题模板
     * 支持变量占位符，如：设备{{deviceName}}{{alertType}}告警
     */
    @TableField("title_template")
    private String titleTemplate;
    
    /**
     * 内容摘要
     * 模板内容的简要描述，便于管理员快速识别
     */
    @TableField("content_summary")
    private String contentSummary;
    
    /**
     * 模板版本号
     * 支持模板版本管理，格式：v1.0.0
     */
    @TableField("version")
    private String version;
    
    /**
     * 模板状态
     * 
     * 枚举值：
     * - DRAFT: 草稿状态
     * - ACTIVE: 激活状态
     * - INACTIVE: 停用状态
     * - DEPRECATED: 已废弃
     */
    @TableField("status")
    private String status;
    
    /**
     * 是否为系统模板
     * true-系统内置模板，不允许删除
     * false-用户自定义模板，可以删除
     */
    @TableField("is_system")
    private Boolean isSystem;
    
    /**
     * 是否启用
     * 控制模板是否可用于消息发送
     */
    @TableField("is_active")
    private Boolean isActive;
    
    /**
     * 使用次数
     * 统计模板的使用频率
     */
    @TableField("usage_count")
    private Long usageCount;
    
    /**
     * 最后使用时间
     * 记录模板最近一次被使用的时间
     */
    @TableField("last_used_time")
    private LocalDateTime lastUsedTime;
    
    /**
     * 创建者ID
     * 模板的创建用户
     */
    @TableField("created_by")
    private String createdBy;
    
    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    
    /**
     * 更新者ID
     * 最后修改模板的用户
     */
    @TableField("updated_by")
    private String updatedBy;
    
    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
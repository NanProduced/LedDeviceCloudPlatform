package org.nan.cloud.message.api.enums;

import lombok.Getter;

/**
 * 消息类型枚举
 * 
 * 定义了平台中所有可能的消息类型，每种类型对应不同的业务场景。
 * 前端可以根据消息类型来决定如何显示和处理消息。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Getter
public enum MessageType {
    
    /**
     * 系统通知
     * 用途: 系统维护、版本更新、公告等系统级消息
     * 示例: "系统将于今晚23:00进行维护，预计1小时"
     */
    SYSTEM_NOTIFICATION("system_notification", "系统通知", "系统级重要通知和公告"),
    
    /**
     * 设备告警
     * 用途: LED设备故障、离线、异常状态等告警信息
     * 示例: "设备LED-001离线超过5分钟"
     */
    DEVICE_ALERT("device_alert", "设备告警", "LED设备状态异常告警"),
    
    /**
     * 设备状态更新
     * 用途: 设备上线、下线、状态变化等正常状态通知
     * 示例: "设备LED-001已上线"
     */
    DEVICE_STATUS("device_status", "设备状态", "设备状态变化通知"),
    
    /**
     * 用户消息
     * 用途: 用户之间的私信、通知等人际交互消息
     * 示例: "管理员张三向您发送了一条消息"
     */
    USER_MESSAGE("user_message", "用户消息", "用户间的私信和通知"),
    
    /**
     * 任务通知
     * 用途: 工作流任务、批处理作业等任务相关通知
     * 示例: "设备配置更新任务已完成"
     */
    TASK_NOTIFICATION("task_notification", "任务通知", "任务执行状态和结果通知"),
    
    /**
     * 权限变更
     * 用途: 用户权限、角色变更等安全相关通知
     * 示例: "您已被授予设备管理权限"
     */
    PERMISSION_CHANGE("permission_change", "权限变更", "用户权限和角色变更通知"),
    
    /**
     * 业务提醒
     * 用途: 业务流程提醒、截止日期提醒等
     * 示例: "设备巡检计划将于明天到期"
     */
    BUSINESS_REMINDER("business_reminder", "业务提醒", "业务流程和事项提醒"),
    
    /**
     * 实时数据
     * 用途: 实时监控数据、图表更新等
     * 示例: 设备实时状态数据推送
     */
    REALTIME_DATA("realtime_data", "实时数据", "实时监控数据推送"),
    
    /**
     * 聊天消息
     * 用途: 在线客服、技术支持等即时通讯
     * 示例: 客服聊天消息
     */
    CHAT_MESSAGE("chat_message", "聊天消息", "即时通讯聊天消息");
    
    /**
     * 消息类型代码
     * 用于API传输和数据库存储的标识符
     */
    private final String code;
    
    /**
     * 消息类型显示名称
     * 用于前端界面显示的中文名称
     */
    private final String displayName;
    
    /**
     * 消息类型描述
     * 详细说明该类型消息的用途和场景
     */
    private final String description;
    
    /**
     * 构造函数
     * 
     * @param code 消息类型代码
     * @param displayName 显示名称  
     * @param description 类型描述
     */
    MessageType(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 根据代码获取消息类型
     * 
     * @param code 消息类型代码
     * @return 对应的消息类型枚举，找不到则返回null
     */
    public static MessageType fromCode(String code) {
        for (MessageType type : MessageType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为高优先级消息类型
     * 高优先级消息需要立即处理和显示
     * 
     * @return true表示高优先级，false表示普通优先级
     */
    public boolean isHighPriority() {
        return this == DEVICE_ALERT || 
               this == SYSTEM_NOTIFICATION || 
               this == PERMISSION_CHANGE;
    }
    
    /**
     * 判断是否需要持久化存储
     * 某些临时消息（如实时数据）可能不需要长期保存
     * 
     * @return true表示需要存储，false表示临时消息
     */
    public boolean requiresPersistence() {
        return this != REALTIME_DATA; // 实时数据通常不需要持久化
    }
}
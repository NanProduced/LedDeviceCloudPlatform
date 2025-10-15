package org.nan.cloud.message.api.enums;

import lombok.Getter;

/**
 * 消息优先级枚举
 * 
 * 定义消息的优先级别，影响消息的处理顺序、显示方式和推送策略。
 * 优先级越高的消息会优先处理和显示。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Getter
public enum Priority {
    
    /**
     * 低优先级
     * 特点: 可以延迟处理，不紧急的通知消息
     * 显示: 普通样式，不会打断用户操作
     * 示例: 系统维护预告、功能介绍等
     */
    LOW(1, "低", "low", "#666666", false),
    
    /**
     * 普通优先级  
     * 特点: 常规业务消息，按正常流程处理
     * 显示: 标准样式，出现在通知列表中
     * 示例: 设备状态更新、任务完成通知等
     */
    NORMAL(2, "普通", "normal", "#1890ff", false),
    
    /**
     * 高优先级
     * 特点: 重要消息，需要及时处理和关注
     * 显示: 突出显示，可能有声音提醒
     * 示例: 设备告警、权限变更、重要任务等
     */
    HIGH(3, "高", "high", "#ff4d4f", true),
    
    /**
     * 紧急优先级
     * 特点: 极其重要，需要立即处理的消息
     * 显示: 强制弹窗显示，持续提醒直到确认
     * 示例: 系统故障、安全告警、紧急维护等
     */
    URGENT(4, "紧急", "urgent", "#ff0000", true);
    
    /**
     * 优先级数值
     * 数值越大优先级越高，用于排序和比较
     */
    private final int level;
    
    /**
     * 优先级显示名称
     * 用于前端界面显示的中文名称
     */
    private final String displayName;
    
    /**
     * 优先级代码
     * 用于API传输和前端CSS类名
     */
    private final String code;
    
    /**
     * 优先级颜色
     * 用于前端显示时的颜色标识，使用十六进制颜色值
     */
    private final String color;
    
    /**
     * 是否需要强制通知
     * true: 需要声音、震动等强制提醒方式
     * false: 普通显示即可
     */
    private final boolean forceNotification;
    
    /**
     * 构造函数
     * 
     * @param level 优先级数值
     * @param displayName 显示名称
     * @param code 优先级代码
     * @param color 显示颜色
     * @param forceNotification 是否强制通知
     */
    Priority(int level, String displayName, String code, String color, boolean forceNotification) {
        this.level = level;
        this.displayName = displayName;
        this.code = code;
        this.color = color;
        this.forceNotification = forceNotification;
    }
    
    /**
     * 根据代码获取优先级
     * 
     * @param code 优先级代码
     * @return 对应的优先级枚举，找不到则返回NORMAL
     */
    public static Priority fromCode(String code) {
        for (Priority priority : Priority.values()) {
            if (priority.getCode().equals(code)) {
                return priority;
            }
        }
        return NORMAL; // 默认返回普通优先级
    }
    
    /**
     * 根据数值获取优先级
     * 
     * @param level 优先级数值
     * @return 对应的优先级枚举，找不到则返回NORMAL
     */
    public static Priority fromLevel(int level) {
        for (Priority priority : Priority.values()) {
            if (priority.getLevel() == level) {
                return priority;
            }
        }
        return NORMAL; // 默认返回普通优先级
    }
    
    /**
     * 比较优先级高低
     * 
     * @param other 另一个优先级
     * @return true表示当前优先级更高
     */
    public boolean isHigherThan(Priority other) {
        return this.level > other.level;
    }
    
    /**
     * 获取推送延迟时间（毫秒）
     * 根据优先级决定消息推送的延迟时间
     * 
     * @return 延迟时间（毫秒）
     */
    public long getPushDelayMs() {
        switch (this) {
            case URGENT:
                return 0; // 立即推送
            case HIGH:
                return 100; // 100毫秒延迟
            case NORMAL:
                return 500; // 500毫秒延迟
            case LOW:
                return 2000; // 2秒延迟
            default:
                return 500;
        }
    }
    
    /**
     * 获取消息保留时间（小时）
     * 不同优先级的消息保留时间不同
     * 
     * @return 保留时间（小时）
     */
    public int getRetentionHours() {
        switch (this) {
            case URGENT:
                return 168; // 7天
            case HIGH:
                return 72;  // 3天
            case NORMAL:
                return 24;  // 1天
            case LOW:
                return 12;  // 12小时
            default:
                return 24;
        }
    }
}
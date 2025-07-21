package org.nan.cloud.message.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 任务通知配置
 * 
 * 配置任务通知的各种策略和参数，支持组织级和用户级的个性化配置。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationConfig {
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 用户ID（可选，为空表示组织默认配置）
     */
    private String userId;
    
    /**
     * 是否启用会话级通知
     */
    private boolean enableSessionNotification;
    
    /**
     * 是否启用用户级通知
     */
    private boolean enableUserNotification;
    
    /**
     * 是否启用离线通知保存
     */
    private boolean enableOfflineNotification;
    
    /**
     * 是否启用进度通知
     */
    private boolean enableProgressNotification;
    
    /**
     * 最大离线通知数量
     */
    private int maxOfflineNotifications;
    
    /**
     * 通知保留天数
     */
    private int notificationRetentionDays;
    
    /**
     * 自定义配置
     */
    private Map<String, Object> customSettings;
}
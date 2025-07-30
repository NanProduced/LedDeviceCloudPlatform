package org.nan.cloud.core.api.DTO.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * STOMP主题权限验证响应DTO
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopicPermissionResponse {
    
    /**
     * 权限验证是否通过
     */
    private Boolean hasPermission;
    
    /**
     * 权限验证失败原因（权限验证失败时返回）
     */
    private String deniedReason;
    
    /**
     * 批量验证结果映射
     * Key: 主题路径
     * Value: 该主题的权限验证结果
     */
    private Map<String, TopicPermissionResult> batchResults;
    
    /**
     * 用户可访问的终端ID列表（当验证终端相关权限时返回）
     */
    private List<String> accessibleTerminalIds;
    
    /**
     * 用户所属的用户组ID列表
     */
    private List<Long> userGroupIds;
    
    /**
     * 验证时间戳
     */
    private Long timestamp;
    
    /**
     * 单个主题权限验证结果
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopicPermissionResult {
        
        /**
         * 主题路径
         */
        private String topicPath;
        
        /**
         * 是否有权限
         */
        private Boolean hasPermission;
        
        /**
         * 权限等级（如：READ, WRITE, ADMIN）
         */
        private String permissionLevel;
        
        /**
         * 拒绝原因（无权限时）
         */
        private String deniedReason;
    }
    
    /**
     * 创建成功响应
     */
    public static TopicPermissionResponse success() {
        return TopicPermissionResponse.builder()
                .hasPermission(true)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建成功响应（带批量结果）
     */
    public static TopicPermissionResponse success(Map<String, TopicPermissionResult> batchResults) {
        return TopicPermissionResponse.builder()
                .hasPermission(true)
                .batchResults(batchResults)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建失败响应
     */
    public static TopicPermissionResponse denied(String reason) {
        return TopicPermissionResponse.builder()
                .hasPermission(false)
                .deniedReason(reason)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
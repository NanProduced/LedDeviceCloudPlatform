package org.nan.cloud.core.api.DTO.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * STOMP主题权限验证请求DTO
 * 
 * @author Nan
 * @since 1.0.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopicPermissionRequest {
    
    /**
     * 用户ID
     */
    private Long uid;
    
    /**
     * 组织ID
     */
    private Long oid;
    
    /**
     * 用户组ID
     */
    private Long ugid;
    
    /**
     * 终端ID（当验证终端相关主题时使用）
     */
    private String tid;

    /**
     * 用户类型
     */
    private Integer userType;
    
    /**
     * 要验证的主题路径（单个主题验证时使用）
     */
    private String topicPath;
    
    /**
     * 要验证的主题路径列表（批量验证时使用）
     */
    private List<String> topicPaths;
    
    /**
     * 主题类型，用于帮助权限验证逻辑分类处理
     * 可选值：USER, ORG, TERMINAL, SYSTEM, BATCH_COMMAND 等
     */
    private String topicType;
    
    /**
     * 请求来源会话ID（用于日志追踪）
     */
    private String sessionId;
}
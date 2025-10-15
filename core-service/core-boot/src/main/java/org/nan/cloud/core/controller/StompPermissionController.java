package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.api.DTO.req.TopicPermissionRequest;
import org.nan.cloud.core.api.DTO.res.TopicPermissionResponse;
import org.nan.cloud.core.handler.StompTopicAuthHandler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * STOMP权限验证控制器
 * 
 * 提供STOMP主题订阅权限验证的REST API接口，供其他服务通过Feign调用。
 * 实现细粒度的主题权限控制，支持用户、组织、终端等不同类型主题的权限验证。
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "STOMP Permission(STOMP RPC调用权限控制器)", description = "STOMP主题权限验证相关操作")
@RestController
@RequestMapping("/rpc/permissions/stomp")
@RequiredArgsConstructor
public class StompPermissionController {
    
    private final StompTopicAuthHandler stompTopicAuthHandler;
    
    /**
     * 验证用户对STOMP主题的订阅权限
     * 
     * @param request 权限验证请求，包含用户信息和主题路径
     * @return 权限验证结果
     */
    @Operation(
        summary = "验证STOMP主题订阅权限",
        description = "验证指定用户是否有权限订阅特定的STOMP主题，支持用户、组织、终端等不同类型主题",
        tags = {"FEIGN_RPC", "STOMP"}
    )
    @PostMapping("/topic/verify")
    public TopicPermissionResponse verifyTopicSubscriptionPermission(
            @RequestBody @Validated TopicPermissionRequest request) {
        
        log.debug("验证STOMP主题权限 - 用户: {}, 主题: {}, 类型: {}", 
                request.getUid(), request.getTopicPath(), request.getTopicType());
        
        return stompTopicAuthHandler.verifyTopicSubscriptionPermission(request);
    }
    
    /**
     * 批量验证用户对多个STOMP主题的订阅权限
     * 用于自动订阅时的批量权限验证
     * 
     * @param request 批量权限验证请求
     * @return 批量权限验证结果
     */
    @Operation(
        summary = "批量验证STOMP主题订阅权限",
        description = "批量验证用户对多个STOMP主题的订阅权限，主要用于用户连接时的自动订阅场景",
        tags = {"FEIGN_RPC", "STOMP"}
    )
    @PostMapping("/topic/batch-verify")
    public TopicPermissionResponse batchVerifyTopicSubscriptionPermission(
            @RequestBody @Validated TopicPermissionRequest request) {
        
        log.debug("批量验证STOMP主题权限 - 用户: {}, 主题数量: {}", 
                request.getUid(), request.getTopicPaths() != null ? request.getTopicPaths().size() : 0);
        
        return stompTopicAuthHandler.batchVerifyTopicSubscriptionPermission(request);
    }
}
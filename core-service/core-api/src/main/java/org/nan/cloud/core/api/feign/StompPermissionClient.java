package org.nan.cloud.core.api.feign;

import org.nan.cloud.core.api.DTO.req.TopicPermissionRequest;
import org.nan.cloud.core.api.DTO.res.TopicPermissionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * STOMP权限验证Feign客户端
 * 
 * 提供STOMP主题订阅权限验证的RPC接口，供其他服务调用。
 * 遵循微服务架构最佳实践：接口定义由服务提供方管理。
 * 
 * @author Nan
 * @since 1.0.0
 */
@FeignClient(
    value = "core-service",
    path = "/rpc/permissions/stomp"
)
public interface StompPermissionClient {
    
    /**
     * 验证用户对STOMP主题的订阅权限
     * 
     * @param request 权限验证请求，包含用户信息和主题路径
     * @return 权限验证结果
     */
    @PostMapping("/topic/verify")
    TopicPermissionResponse verifyTopicSubscriptionPermission(@RequestBody TopicPermissionRequest request);
    
    /**
     * 批量验证用户对多个STOMP主题的订阅权限
     * 用于自动订阅时的批量权限验证
     * 
     * @param request 批量权限验证请求
     * @return 批量权限验证结果
     */
    @PostMapping("/topic/batch-verify")
    TopicPermissionResponse batchVerifyTopicSubscriptionPermission(@RequestBody TopicPermissionRequest request);
}
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
 * <p>区别纯内部使用的RPC feign</p>
 * <p>内部\外部RESTful通用 -> client包</p>
 * <p>内部服务间使用 -> feign包</p>
 * @author Nan
 * @since 1.0.0
 */
@FeignClient(value = "core-service")
public interface StompPermissionClient {

    String prefix = "/rpc/permissions/stomp";
    
    /**
     * 验证用户对STOMP主题的订阅权限
     * 
     * @param request 权限验证请求，包含用户信息和主题路径
     * @return 权限验证结果
     */
    @PostMapping(prefix + "/topic/verify")
    TopicPermissionResponse verifyTopicSubscriptionPermission(@RequestBody TopicPermissionRequest request);
    
    /**
     * 批量验证用户对多个STOMP主题的订阅权限
     * 用于自动订阅时的批量权限验证
     * 
     * @param request 批量权限验证请求
     * @return 批量权限验证结果
     */
    @PostMapping(prefix + "/topic/batch-verify")
    TopicPermissionResponse batchVerifyTopicSubscriptionPermission(@RequestBody TopicPermissionRequest request);
}
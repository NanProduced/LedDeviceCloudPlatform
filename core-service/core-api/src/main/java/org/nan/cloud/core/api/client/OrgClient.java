package org.nan.cloud.core.api.client;

import org.nan.cloud.core.api.OrgApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 区别纯内部使用的RPC feign
 * <p>内部\外部RESTful通用 -> client包</p>
 * <p>内部服务间使用 -> feign包</p>
 */
@FeignClient(value = "core-service")
public interface OrgClient extends OrgApi {
}

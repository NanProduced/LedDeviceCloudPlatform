package org.nan.cloud.core.api.client;

import org.nan.cloud.core.api.OrgApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(value = "core-service")
public interface OrgClient extends OrgApi {
}

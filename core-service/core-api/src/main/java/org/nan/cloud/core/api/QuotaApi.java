package org.nan.cloud.core.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.core.api.DTO.req.QuotaCheckRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface QuotaApi {

    String PREFIX = "/quota";

    @PostMapping(PREFIX + "/check")
    Boolean checkQuota(@RequestBody QuotaCheckRequest request);
}


package org.nan.cloud.core.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nan.cloud.core.api.quota.dto.QuotaCheckRequest;
import org.nan.cloud.core.api.quota.dto.QuotaCheckResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Quota(组织配额)", description = "组织配额相关内部接口")
public interface QuotaApi {

    String PREFIX = "/internal/quota";

    @Operation(summary = "配额检查", description = "上传前检查组织配额是否充足（仅内部调用）")
    @PostMapping(PREFIX + "/check")
    QuotaCheckResponse checkQuota(@RequestBody QuotaCheckRequest request);
}


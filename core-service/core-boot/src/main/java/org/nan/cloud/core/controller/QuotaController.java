package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.QuotaApi;
import org.nan.cloud.core.api.DTO.req.QuotaCheckRequest;
import org.nan.cloud.core.facade.OrgFacade;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quota(组织配额)", description = "组织配额相关内部接口")
public class QuotaController implements QuotaApi {

    private final OrgFacade  orgFacade;

    @Operation(
            summary = "配额检查",
            description = "上传前检查组织配额是否充足（仅内部调用）",
            tags = {"内部接口"}
    )
    @Override
    public Boolean checkQuota(QuotaCheckRequest request) {

        return orgFacade.checkOrgQuota(request);
    }
}


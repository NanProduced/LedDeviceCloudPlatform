package org.nan.cloud.core.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.QuotaApi;
import org.nan.cloud.core.api.DTO.req.QuotaCheckRequest;
import org.nan.cloud.core.facade.OrgFacade;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QuotaController implements QuotaApi {

    private final OrgFacade  orgFacade;

    @Override
    public Boolean checkQuota(QuotaCheckRequest request) {

        return orgFacade.checkOrgQuota(request);
    }
}


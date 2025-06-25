package org.nan.cloud.core.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.api.OrgApi;
import org.nan.cloud.core.facade.OrgFacade;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrgController implements OrgApi {

    private final OrgFacade orgFacade;

    @Override
    public CreateOrgResponse createOrg(CreateOrgRequest request) {
        return orgFacade.createOrg(request);
    }
}

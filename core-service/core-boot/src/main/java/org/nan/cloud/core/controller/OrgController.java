package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.api.OrgApi;
import org.nan.cloud.core.facade.OrgFacade;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Organization(组织)", description = "组织相关的所有操作")
@RestController
@RequiredArgsConstructor
public class OrgController implements OrgApi {

    private final OrgFacade orgFacade;

    @Operation(
            summary = "创建组织",
            description = "超级管理员/系统管理员创建组织",
            tags = {"组织管理"}
    )
    @Override
    public CreateOrgResponse createOrg(CreateOrgRequest request) {
        return orgFacade.createOrg(request);
    }
}

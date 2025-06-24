package org.nan.cloud.core.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.context.InvocationContextHolder;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.api.OrgApi;
import org.nan.cloud.core.converter.OrgConverter;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.service.OrgService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrgController implements OrgApi {

    private final OrgService orgService;

    private final OrgConverter orgConverter;

    @Override
    public CreateOrgResponse createOrg(CreateOrgRequest request) {
        final Long currentUId = InvocationContextHolder.getCurrentUId();
        final CreateOrgDTO createOrgDTO = orgConverter.convert2CreateOrgDTO(request);
        final Organization org = orgService.createOrg(createOrgDTO, currentUId);
        return orgConverter.convert2CreateOrgRes(org);
    }
}

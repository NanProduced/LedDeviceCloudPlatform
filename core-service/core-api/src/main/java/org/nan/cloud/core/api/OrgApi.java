package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/core/api/org")
public interface OrgApi {

    @PostMapping("/create")
    CreateOrgResponse createOrg(@Validated @RequestBody CreateOrgRequest request);


}

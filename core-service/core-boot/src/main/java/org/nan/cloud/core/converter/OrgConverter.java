package org.nan.cloud.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.DTO.CreateOrgVO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OrgConverter {

    CreateOrgDTO createOrgRequest2CreateOrgDTO(CreateOrgRequest request);

    CreateOrgResponse createOrgResult2CreateOrgResponse(CreateOrgVO result);
}

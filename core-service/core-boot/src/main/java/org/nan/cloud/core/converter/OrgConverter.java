package org.nan.cloud.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.DTO.CreateOrgDTO;
import org.nan.cloud.core.api.DTO.req.CreateOrgRequest;
import org.nan.cloud.core.api.DTO.res.CreateOrgResponse;
import org.nan.cloud.core.domain.Organization;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface OrgConverter {

    CreateOrgDTO convert2CreateOrgDTO(CreateOrgRequest request);

    CreateOrgResponse convert2CreateOrgRes(Organization organization);
}

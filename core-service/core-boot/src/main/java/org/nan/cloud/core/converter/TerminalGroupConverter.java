package org.nan.cloud.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.api.DTO.common.OrganizationDTO;
import org.nan.cloud.core.api.DTO.res.TerminalGroupDetailResponse;
import org.nan.cloud.core.api.DTO.res.TerminalGroupListResponse;
import org.nan.cloud.core.domain.Organization;
import org.nan.cloud.core.domain.TerminalGroup;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface TerminalGroupConverter {

    @Mapping(source = "name", target = "terminalGroupName")
    TerminalGroupDetailResponse terminalGroup2DetailResponse(TerminalGroup terminalGroup);

    @Mapping(source = "name", target = "terminalGroupName")
    TerminalGroupListResponse terminalGroup2ListResponse(TerminalGroup terminalGroup);

    @Mapping(source = "name", target = "organizationName")
    OrganizationDTO organization2OrganizationDTO(Organization organization);
}
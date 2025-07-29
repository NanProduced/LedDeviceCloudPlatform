package org.nan.cloud.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.api.DTO.res.RoleDetailResponse;
import org.nan.cloud.core.domain.Role;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface RoleConverter {

    RoleDetailResponse toRoleDetailResponse(Role role);
}

package org.nan.cloud.core.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.nan.cloud.core.api.DTO.res.OperationPermissionResponse;
import org.nan.cloud.core.api.DTO.res.RoleDetailResponse;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.domain.Role;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface RoleAndPermissionConverter {

    @Mapping(source = "name", target = "roleName")
    RoleDetailResponse toRoleDetailResponse(Role role);

    @Mapping(source = "name", target = "operationName")
    @Mapping(source = "description", target = "operationDescription")
    OperationPermissionResponse toOperationPermissionResponse(OperationPermission operationPermission);

    List<OperationPermissionResponse> toOperationPermissionResponse(List<OperationPermission> operationPermissions);
}

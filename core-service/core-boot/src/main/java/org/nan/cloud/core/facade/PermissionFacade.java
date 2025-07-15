package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.api.DTO.res.PermissionResponse;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.enums.UserTypeEnum;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PermissionFacade {

    private final RoleAndPermissionService roleAndPermissionService;

    public Map<String, List<PermissionResponse>> getCurrentUserPermissions() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        List<Permission> permissions;
        if (requestUser.getUserType().equals(UserTypeEnum.ORG_MANAGER_USER.getCode())) {
            permissions = roleAndPermissionService.getAllPermissions();
        }
        else {
            permissions = roleAndPermissionService.getPermissionsByUid(requestUser.getOid(), requestUser.getUid());
        }
        return permissions.stream().map(p ->
                new PermissionResponse(p.getPermissionId(), p.getName(), p.getDescription(), p.getGroup()))
                .collect(Collectors.groupingBy(PermissionResponse::getPermissionType, Collectors.toList()));
    }
}

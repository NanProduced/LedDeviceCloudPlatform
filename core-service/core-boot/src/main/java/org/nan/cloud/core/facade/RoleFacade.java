package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.service.PermissionEventPublisher;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleFacade {

    private final PermissionEventPublisher  permissionEventPublisher;

    private final RoleAndPermissionService roleAndPermissionService;

    @Transactional
    public void createRole(CreateRoleRequest request) {
        final Long currentUId = InvocationContextHolder.getCurrentUId();
        final Long oid = InvocationContextHolder.getOid();
        // 获取权限
        final List<Permission> permissions = roleAndPermissionService.getPermissionsByIds(request.getPermissions());
        ExceptionEnum.PARAM_PARAMETER_EXCEPTION.throwIf(permissions.size() != request.getPermissions().size());
        // 创建角色
        final Role role = roleAndPermissionService.createRole(Role.builder()
                .oid(oid)
                .name(request.getRoleName())
                .creatorId(currentUId)
                .type(0)
                .build());
        ExceptionEnum.CREATE_FAILED.throwIf(null == role.getRid());
        final Long rid = role.getRid();
        roleAndPermissionService.createRolePermissionRel(rid, permissions.stream().map(Permission::getPermissionId).collect(Collectors.toSet()));
        permissionEventPublisher.publishAddRoleAndPermissionRelEvent(rid, oid, permissions);
    }

    void assignRolesToUser() {

    }
}

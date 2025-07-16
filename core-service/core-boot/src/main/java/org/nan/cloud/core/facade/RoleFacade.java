package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.basic.utils.StringUtils;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.common.web.context.RequestUserInfo;
import org.nan.cloud.core.DTO.UpdateRoleDTO;
import org.nan.cloud.core.api.DTO.common.RoleDTO;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.api.DTO.req.UpdateRolesRequest;
import org.nan.cloud.core.api.DTO.res.VisibleRolesResponse;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.service.PermissionChecker;
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

    private final PermissionChecker permissionChecker;

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
                .name(StringUtils.generateOrgRoleName(oid))
                .displayName(request.getRoleName())
                .creatorId(currentUId)
                .type(0)
                .build());
        ExceptionEnum.CREATE_FAILED.throwIf(null == role.getRid());
        final Long rid = role.getRid();
        roleAndPermissionService.createRolePermissionRel(rid, permissions.stream().map(Permission::getPermissionId).collect(Collectors.toSet()));
        permissionEventPublisher.publishAddRoleAndPermissionRelEvent(rid, oid, permissions);
    }

    public VisibleRolesResponse getVisibleRoles() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        List<Role> visibleRoles = roleAndPermissionService.getVisibleRolesByUid(requestUser.getOid(), requestUser.getUid());
        return VisibleRolesResponse.builder()
                .uid(requestUser.getUid())
                .visibleRoles(visibleRoles.stream().map(r -> new RoleDTO(r.getRid(), r.getOid(), r.getName(), r.getDisplayName())).toList())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void updateRole(UpdateRolesRequest updateRolesRequest) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!permissionChecker.ifTargetRoleIsTheSameOrg(requestUser.getOid(), updateRolesRequest.getRid()));
        ExceptionEnum.ROLE_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRole(requestUser.getUid(), updateRolesRequest.getRid()));
        UpdateRoleDTO dto = UpdateRoleDTO.builder()
                .rid(updateRolesRequest.getRid())
                .roleName(updateRolesRequest.getName())
                .description(updateRolesRequest.getDescription())
                .updaterUid(requestUser.getUid())
                .build();
        if (dto.needToUpdateRole()) {
            roleAndPermissionService.updateRole(dto);
        }
        // 覆盖角色权限
        if (CollectionUtils.isNotEmpty(updateRolesRequest.getPermissionIds())) {
            List<Long> permissionIds = updateRolesRequest.getPermissionIds();
            List<Permission> permissions = roleAndPermissionService.getPermissionsByIds(permissionIds);
            ExceptionEnum.PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetPermissions(requestUser.getUid(), permissionIds));
            permissionEventPublisher.publishChangeRoleAndPermissionRelEvent(updateRolesRequest.getRid(), requestUser.getOid(), permissions);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void deleteRole(Long rid) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!permissionChecker.ifTargetRoleIsTheSameOrg(requestUser.getOid(), rid));
        ExceptionEnum.ROLE_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRole(requestUser.getUid(), rid));
        roleAndPermissionService.deleteRole(requestUser.getOid(), rid);
        permissionEventPublisher.publishRemoveRoleEvent(rid, requestUser.getOid());
    }



}

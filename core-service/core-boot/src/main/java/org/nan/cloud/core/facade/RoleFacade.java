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
import org.nan.cloud.core.api.DTO.res.RoleDetailResponse;
import org.nan.cloud.core.api.DTO.res.VisibleRolesResponse;
import org.nan.cloud.core.aspect.SkipOrgManagerPermissionCheck;
import org.nan.cloud.core.converter.RoleAndPermissionConverter;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.enums.RoleTypeEnum;
import org.nan.cloud.core.service.PermissionChecker;
import org.nan.cloud.core.service.PermissionEventPublisher;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.nan.cloud.core.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleFacade {

    private final PermissionEventPublisher  permissionEventPublisher;

    private final RoleAndPermissionService roleAndPermissionService;

    private final PermissionChecker permissionChecker;
    private final RoleAndPermissionConverter roleAndPermissionConverter;

    @Transactional
    public void createRole(CreateRoleRequest request) {
        final Long currentUId = InvocationContextHolder.getCurrentUId();
        final Long oid = InvocationContextHolder.getOid();
        // 获取权限
        final Set<Long> visibleOperationPermissionIds = roleAndPermissionService.getOperationPermissionIdByUid(currentUId);
        ExceptionEnum.PARAM_PARAMETER_EXCEPTION.throwIf(visibleOperationPermissionIds.containsAll(request.getOperationPermissions()));
        // 创建角色
        final Role role = roleAndPermissionService.createRole(Role.builder()
                .oid(oid)
                .name(StringUtils.generateOrgRoleName(oid))
                .displayName(request.getRoleName())
                .creatorId(currentUId)
                .roleType(RoleTypeEnum.CUSTOM_ROLE.getType())
                .build());
        ExceptionEnum.CREATE_FAILED.throwIf(null == role.getRid());
        final Long rid = role.getRid();
        List<Permission> permissions = roleAndPermissionService.getPermissionsByOperationPermissionIds(request.getOperationPermissions());
        roleAndPermissionService.createRoleOperationPermissionRel(rid, new HashSet<>(request.getOperationPermissions()));
        permissionEventPublisher.publishAddRoleAndPermissionRelEvent(rid, oid, permissions);
    }

    @SkipOrgManagerPermissionCheck
    public RoleDetailResponse getRoleDetail(Long rid) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ROLE_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRole(requestUser.getOid(), requestUser.getUid(), rid));
        Role role = roleAndPermissionService.getRoleByRid(rid, true);
        List<OperationPermission> operationPermission = roleAndPermissionService.getOperationPermissionByRid(rid);
        RoleDetailResponse roleDetailResponse = roleAndPermissionConverter.toRoleDetailResponse(role);
        roleDetailResponse.setOperationPermissions(roleAndPermissionConverter.toOperationPermissionResponse(operationPermission));
        return roleDetailResponse;
    }

    public VisibleRolesResponse getVisibleRoles() {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        List<Role> visibleRoles;
        if (InvocationContextHolder.ifOrgManager()) {
            visibleRoles = roleAndPermissionService.getAllRolesByOid(requestUser.getOid());
        }
        else {
            visibleRoles = roleAndPermissionService.getVisibleRolesByUid(requestUser.getOid(), requestUser.getUid());
        }
        return VisibleRolesResponse.builder()
                .uid(requestUser.getUid())
                .visibleRoles(visibleRoles.stream().map(r -> new RoleDTO(r.getRid(), r.getOid(), r.getName(), r.getDisplayName())).toList())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void updateRole(UpdateRolesRequest updateRolesRequest) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ROLE_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRole(requestUser.getOid(), requestUser.getUid(), updateRolesRequest.getRid()));
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
        if (CollectionUtils.isNotEmpty(updateRolesRequest.getOperationPermissionIds())) {
            List<Long> operationPermissionIds = updateRolesRequest.getOperationPermissionIds();
            // 获取权限
            final Set<Long> visibleOperationPermissionIds = roleAndPermissionService.getOperationPermissionIdByUid(requestUser.getUid());
            ExceptionEnum.PARAM_PARAMETER_EXCEPTION.throwIf(visibleOperationPermissionIds.containsAll(operationPermissionIds));
            List<Permission> permissions = roleAndPermissionService.getPermissionsByOperationPermissionIds(operationPermissionIds);
            roleAndPermissionService.updateRoleOperationPermissionRel(updateRolesRequest.getRid(), operationPermissionIds);
            permissionEventPublisher.publishChangeRoleAndPermissionRelEvent(updateRolesRequest.getRid(), requestUser.getOid(), permissions);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipOrgManagerPermissionCheck
    public void deleteRole(Long rid) {
        RequestUserInfo requestUser = InvocationContextHolder.getContext().getRequestUser();
        ExceptionEnum.ROLE_PERMISSION_DENIED.throwIf(!permissionChecker.ifHasPermissionOnTargetRole(requestUser.getOid(), requestUser.getUid(), rid));
        roleAndPermissionService.deleteRole(requestUser.getOid(), rid);
        permissionEventPublisher.publishRemoveRoleEvent(rid, requestUser.getOid());
    }



}

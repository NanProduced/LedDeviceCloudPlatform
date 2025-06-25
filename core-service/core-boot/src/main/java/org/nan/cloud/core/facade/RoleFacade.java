package org.nan.cloud.core.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.casbin.CasbinRbacPolicyHandler;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleFacade {

    private final CasbinRbacPolicyHandler rbacPolicyHandler;

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
        permissions.forEach(p -> {
            final boolean b = rbacPolicyHandler.addRolePolicy(rid, oid, p.getUrl(), p.getMethod());
            if (!b) {
                log.warn("rbac add policy failed: role:{}--permission:{}", role, p);
                ExceptionEnum.SERVER_ERROR.throwThis();
            }
        });
    }

    void assignRolesToUser() {

    }
}

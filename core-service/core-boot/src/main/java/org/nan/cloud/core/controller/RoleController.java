package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.req.UpdateRolesRequest;
import org.nan.cloud.core.api.DTO.res.VisibleRolesResponse;
import org.nan.cloud.core.api.RoleApi;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.facade.RoleFacade;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Role(角色控制器)", description = "角色相关的所有操作")
@RestController
@RequiredArgsConstructor
public class RoleController implements RoleApi {

    private final RoleFacade roleFacade;

    @Operation(
            summary = "创建角色",
            description = "组织内创建角色，用于分配权限",
            tags = {"角色管理"}
    )
    @Override
    public void createRole(CreateRoleRequest request) {
        roleFacade.createRole(request);
    }

    @Operation(
            summary = "获取当前用户有权限的角色",
            description = "查询当前用户有权限的组织内角色，用于分配",
            tags = {"用户信息", "通用接口"}
    )
    @Override
    public VisibleRolesResponse getVisibleRoles() {
        return roleFacade.getVisibleRoles();
    }

    @Operation(
            summary = "更新指定角色",
            description = "更新指定角色信息或重置权限",
            tags = {"角色管理"}
    )
    @Override
    public void updateRoles(UpdateRolesRequest request) {
        roleFacade.updateRole(request);
    }
}

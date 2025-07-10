package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.RoleApi;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.facade.RoleFacade;
import org.springframework.web.bind.annotation.RestController;

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
}

package org.nan.cloud.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.DTO.res.PermissionResponse;
import org.nan.cloud.core.api.PermissionApi;
import org.nan.cloud.core.facade.PermissionFacade;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Permission(权限控制器)", description = "权限相关操作")
@RestController
@RequiredArgsConstructor
public class PermissionController implements PermissionApi {

    private final PermissionFacade permissionFacade;

    @Operation(
            summary = "获取当前用户可用权限",
            description = "根据当前用户角色获取用户可用的全部权限",
            tags = {"权限管理","通用接口"}
    )
    @Override
    public Map<String, List<PermissionResponse>> getCurUserPermissions() {
        return permissionFacade.getCurrentUserPermissions();
    }
}

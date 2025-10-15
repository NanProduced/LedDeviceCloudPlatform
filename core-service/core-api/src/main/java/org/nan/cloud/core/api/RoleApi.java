package org.nan.cloud.core.api;

import jakarta.validation.constraints.NotNull;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.api.DTO.req.UpdateRolesRequest;
import org.nan.cloud.core.api.DTO.res.RoleDetailResponse;
import org.nan.cloud.core.api.DTO.res.VisibleRolesResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RoleApi {

    String prefix = "/role";

    @PostMapping(prefix + "/create")
    void createRole(@Validated @RequestBody CreateRoleRequest request);

    @GetMapping(prefix + "/detail")
    RoleDetailResponse getRoleDetail(@RequestParam("rid") @NotNull Long rid);

    @GetMapping(prefix + "/get/visible")
    VisibleRolesResponse getVisibleRoles();

    @PostMapping(prefix + "/update")
    void updateRoles(@Validated @RequestBody UpdateRolesRequest request);

    @PostMapping(prefix + "/delete")
    void deleteRole(@RequestParam("rid") Long rid);

}

package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface RoleApi {

    String prefix = "/role";

    @PostMapping(prefix + "/create")
    void createRole(@Validated @RequestBody CreateRoleRequest request);
}

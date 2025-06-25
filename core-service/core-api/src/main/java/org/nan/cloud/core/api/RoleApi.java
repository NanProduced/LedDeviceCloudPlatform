package org.nan.cloud.core.api;

import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.springframework.web.bind.annotation.PostMapping;

public interface RoleApi {

    String prefix = "core/api/role";

    @PostMapping(prefix + "/create")
    void createRole(CreateRoleRequest request);
}

package org.nan.cloud.core.api.DTO;

import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/core/api/role")
public interface RoleApi {

    @PostMapping("/create")
    void createRole(CreateRoleRequest request);
}

package org.nan.cloud.core.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.api.RoleApi;
import org.nan.cloud.core.api.DTO.req.CreateRoleRequest;
import org.nan.cloud.core.facade.RoleFacade;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RoleController implements RoleApi {

    private final RoleFacade roleFacade;

    @Override
    public void createRole(CreateRoleRequest request) {
        roleFacade.createRole(request);
    }
}

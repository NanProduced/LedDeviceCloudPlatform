package org.nan.cloud.core.api.DTO.req;

import lombok.Data;

import java.util.List;

@Data
public class CreateRoleRequest {

    private String roleName;

    private List<Long> permissions;
}

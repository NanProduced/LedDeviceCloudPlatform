package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;

import java.util.List;

public interface RoleAndPermissionService {

    Role createRole(Role role);

    List<Permission> getPermissionsByIds(List<Long> permissionIds);


}

package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;

import java.util.List;
import java.util.Set;

public interface RoleAndPermissionService {

    Role createRole(Role role);

    void createRolePermissionRel(Long rid, Set<Long> permissionIds);

    List<Permission> getPermissionsByIds(List<Long> permissionIds);

    List<Permission> getPermissionsByRoles(Long oid, List<Long> rids);

    void assignPermissionsToRole(Role role, List<Permission> permissions);

    void assignRoleToUser(Role role, User user);
}

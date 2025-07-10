package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.repository.PermissionRepository;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleAndPermissionServiceImpl implements RoleAndPermissionService {

    private final PermissionRepository permissionRepository;

    @Override
    public Role createRole(Role role) {
        return null;
    }

    @Override
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        return permissionRepository.getPermissionsByIds(permissionIds);
    }

    @Override
    public void assignPermissionsToRole(Role role, List<Permission> permissions) {

    }

    @Override
    public void assignRoleToUser(Role role, User user) {

    }
}

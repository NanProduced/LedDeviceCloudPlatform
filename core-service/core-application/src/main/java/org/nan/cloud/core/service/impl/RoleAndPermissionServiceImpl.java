package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;
import org.nan.cloud.core.repository.PermissionRepository;
import org.nan.cloud.core.repository.RoleRepository;
import org.nan.cloud.core.service.RoleAndPermissionService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAndPermissionServiceImpl implements RoleAndPermissionService {

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    @Override
    public Role createRole(Role role) {
        return roleRepository.createRole(role);
    }

    @Override
    public void createRolePermissionRel(Long rid, Set<Long> permissionIds) {
        permissionRepository.insertRolePermissionRel(rid, permissionIds);
    }

    @Override
    public List<Permission> getPermissionsByIds(List<Long> permissionIds) {
        return permissionRepository.getPermissionsByIds(permissionIds);
    }

    @Override
    public List<Permission> getPermissionsByRoles(Long oid, List<Long> rids) {
        return List.of();
    }

    @Override
    public Map<Long, List<Role>> getRolesByUserIds(List<Long> userIds) {
        Map<Long, List<Long>> userRoleMap = roleRepository.getRoleIdsByUserIds(userIds);
        Set<Long> roleSet = userRoleMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, Role> roleMap = roleRepository.getRolesByRids(roleSet).stream()
                .collect(Collectors.toMap(Role::getRid, Function.identity()));
        Map<Long, List<Role>> result = new HashMap<>(userRoleMap.size());
        userRoleMap.forEach((uid, rids) -> {
            List<Role> roles = rids.stream()
                    .map(roleMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            result.put(uid, roles);
        });
        return result;
    }

    @Override
    public void assignPermissionsToRole(Role role, List<Permission> permissions) {

    }

    @Override
    public void assignRoleToUser(Role role, User user) {

    }
}

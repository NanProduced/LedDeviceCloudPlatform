package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.repository.PermissionRepository;
import org.nan.cloud.core.repository.RoleRepository;
import org.nan.cloud.core.repository.UserGroupRepository;
import org.nan.cloud.core.service.PermissionChecker;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PermissionCheckerImpl implements PermissionChecker {

    private final UserGroupRepository userGroupRepository;

    private final PermissionRepository permissionRepository;

    private final RoleRepository roleRepository;

    /**
     * 检查当前用户组是否对指定的目标用户组拥有权限。
     *
     * @param curUgid 当前用户组的ID
     * @param targetUgid 目标用户组的ID
     * @return 如果当前用户组对目标用户组有权限，则返回true；否则返回false。
     */
    @Override
    public boolean ifHasPermissionOnTargetUserGroup(Long curUgid, Long targetUgid) {
        return userGroupRepository.isAncestorOrSibling(curUgid, targetUgid);
    }

    @Override
    public boolean ifHasPermissionOnTargetRoles(Long oid, Long uid, List<Long> targetRoles) {
        Set<Long> curPermissions = permissionRepository.getPermissionIdsByUid(uid);
        Set<Long> targetPermissions = permissionRepository.getPermissionIdsByRoles(oid, targetRoles);
        return curPermissions.containsAll(targetPermissions);
    }

    @Override
    public boolean ifRolesExist(List<Long> roles) {
        return roleRepository.allRolesExist(roles);
    }
}

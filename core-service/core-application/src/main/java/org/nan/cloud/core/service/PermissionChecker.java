package org.nan.cloud.core.service;

import java.util.List;

public interface PermissionChecker {

    /**
     * 当前用户组是否对指定用户组有权限
     * @param curUgid 当前用户组
     * @param targetUgid 指定用户组
     * @return
     */
    boolean ifHasPermissionOnTargetUserGroup(Long curUgid, Long targetUgid);

    boolean ifHasPermissionOnTargetUser(Long curUid, Long targetUid);

    boolean ifHasPermissionOnTargetRoles(Long oid, Long uid, List<Long> targetRoles);

    boolean ifHasPermissionOnTargetRole(Long uid, Long targetRole);

    boolean ifHasPermissionOnTargetPermissions(Long uid, List<Long> permissionIds);

    boolean ifRolesExist(List<Long> roles);

    boolean ifTargetUserIsTheSameOrg(Long oid, Long targetUid);

    boolean ifTargetRoleIsTheSameOrg(Long oid, Long targetRid);
}

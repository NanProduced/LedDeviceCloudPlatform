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

    boolean ifHasPermissionOnTargetRoles(Long oid, Long uid, List<Long> targetRoles);

    boolean ifRolesExist(List<Long> roles);
}

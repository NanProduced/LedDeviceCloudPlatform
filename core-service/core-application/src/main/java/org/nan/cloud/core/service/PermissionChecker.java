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

    boolean ifHasPermissionOnTargetTerminalGroup(Long ugid, Long targetTgid);

    boolean ifRolesExist(List<Long> roles);

    boolean ifTargetUserIsTheSameOrg(Long oid, Long targetUid);

    boolean ifTargetRoleIsTheSameOrg(Long oid, Long targetRid);

    boolean ifTargetTerminalGroupTheSameOrg(Long oid, Long targetTgid);

    /**
     * 检查用户是否有终端组访问权限
     * 需要通过用户的用户组来检查权限
     */
    boolean hasTerminalGroupAccessPermission(Long uid, Long tgid);
    
    /**
     * 检查操作用户是否有权限修改目标用户组的终端组绑定
     * 包含双重校验：层级校验、终端组权限校验
     */
    void canModifyUserGroupTerminalGroupBinding(Long operatorUid, Long operatorUgid, Long targetUgid, Long targetTgid);

}

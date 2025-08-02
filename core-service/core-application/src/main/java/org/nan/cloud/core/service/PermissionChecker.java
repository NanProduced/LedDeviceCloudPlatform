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

    boolean ifHasPermissionOnTargetRole(Long oid, Long uid, Long targetRole);

    boolean ifHasPermissionOnTargetTerminalGroup(Long ugid, Long targetTgid);

    boolean ifHasPermissionOnTargetTerminalGroup(Long oid, Long ugid, Long targetTgid);

    boolean ifHasPermissionOnTargetFolder(Long oid, Long ugid, Long targetFid);

    boolean ifRolesExist(List<Long> roles);

    boolean ifTargetUserIsTheSameOrg(Long oid, Long targetUid);

    boolean ifTargetRoleIsTheSameOrg(Long oid, Long targetRid);

    boolean ifTargetUserGroupIsTheSameOrg(Long oid, Long targetUgid);

    boolean ifTargetTerminalGroupTheSameOrg(Long oid, Long targetTgid);

    boolean ifTargetFolderTheSameOrg(Long oid, Long targetFid);
    /**
     * 检查操作用户是否有权限修改目标用户组的终端组绑定
     * 包含双重校验：层级校验、终端组权限校验
     */
    void canModifyUserGroupTerminalGroupBinding(Long oid, Long operatorUgid, Long targetUgid, List<Long> targetTgid);

    /**
     * 检查操作用户是否有权限查看目标用户组的终端组绑定
     * 包含基本的层级校验
     */
    void canViewUserGroupTerminalGroupBinding(Long oid, Long operatorUgid, Long targetUgid);

}

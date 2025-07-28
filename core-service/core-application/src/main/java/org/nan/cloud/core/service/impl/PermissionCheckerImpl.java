package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.manager.PermissionCheckSkipContext;
import org.nan.cloud.core.repository.*;
import org.nan.cloud.core.service.PermissionChecker;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionCheckerImpl implements PermissionChecker {

    private final UserGroupRepository userGroupRepository;

    private final UserRepository userRepository;

    private final PermissionRepository permissionRepository;

    private final OperationPermissionRepository operationPermissionRepository;

    private final RoleRepository roleRepository;

    private final TerminalGroupRepository terminalGroupRepository;
    
    private final UserGroupTerminalGroupBindingRepository bindingRepository;

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
    public boolean ifHasPermissionOnTargetUser(Long curUid, Long targetUid) {
        return userRepository.isAncestorOrSiblingByUser(curUid, targetUid);
    }

    @Override
    public boolean ifHasPermissionOnTargetRoles(Long oid, Long uid, List<Long> targetRoles) {
        if (PermissionCheckSkipContext.isSkip()) return true;
        List<Long> rids = roleRepository.getRidsByUid(uid);
        Set<Long> curPermissions = new HashSet<>(operationPermissionRepository.getOperationPermissionIdByRids(rids));
        List<Long> targetPermissions = operationPermissionRepository.getOperationPermissionIdByRids(targetRoles);
        return curPermissions.containsAll(targetPermissions);
    }

    @Override
    public boolean ifHasPermissionOnTargetRole(Long uid, Long targetRole) {
        if (PermissionCheckSkipContext.isSkip()) return true;
        List<Long> rids = roleRepository.getRidsByUid(uid);
        Set<Long> curPermissions = new HashSet<>(operationPermissionRepository.getOperationPermissionIdByRids(rids));
        List<Long> targetPermissions = operationPermissionRepository.getOperationPermissionByRid(targetRole).stream().map(OperationPermission::getOperationPermissionId).toList();
        return curPermissions.containsAll(targetPermissions);
    }

    @Override
    public boolean ifHasPermissionOnTargetTerminalGroup(Long ugid, Long targetTgid) {
        if (PermissionCheckSkipContext.isSkip()) return true;
        // 检查用户组是否对目标终端组有权限
        // 权限检查逻辑：用户组需要直接绑定目标终端组，或者绑定了目标终端组的父组且includeChildren=true
        return bindingRepository.hasTerminalGroupPermission(ugid, targetTgid);
    }

    @Override
    public boolean ifHasPermissionOnTargetTerminalGroup(Long oid, Long ugid, Long targetTgid) {
        if (PermissionCheckSkipContext.isSkip()) return ifTargetTerminalGroupTheSameOrg(oid, targetTgid);
        return bindingRepository.hasTerminalGroupPermission(ugid, targetTgid);
    }

    @Override
    public boolean ifRolesExist(List<Long> roles) {
        return roleRepository.allRolesExist(roles);
    }

    @Override
    public boolean ifTargetUserIsTheSameOrg(Long oid, Long targetUid) {
        return userRepository.ifTheSameOrg(oid,  targetUid);
    }

    @Override
    public boolean ifTargetRoleIsTheSameOrg(Long oid, Long targetRid) {
        return roleRepository.ifTheSameOrg(oid, targetRid);
    }

    @Override
    public boolean ifTargetUserGroupIsTheSameOrg(Long oid, Long targetUgid) {
        return userGroupRepository.ifTheSameOrg(oid, targetUgid);
    }

    @Override
    public boolean ifTargetTerminalGroupTheSameOrg(Long oid, Long targetTgid) {
        return terminalGroupRepository.ifTheSameOrg(oid, targetTgid);
    }
    
    @Override
    public void canModifyUserGroupTerminalGroupBinding(Long oid, Long operatorUgid, Long targetUgid, List<Long> targetTgids) {
        // 组织管理员验证操作的终端组是否是自己组织的
        if (PermissionCheckSkipContext.isSkip()) {
            ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!terminalGroupRepository.ifTheSameOrg(oid, targetTgids));
        }
        else {
            // 1. 操作用户层级校验：操作用户所属的用户组必须是目标用户组的上级用户组
            ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(!userGroupRepository.isAncestor(operatorUgid, targetUgid));
            // 2. 操作用户对目标终端组的权限校验：操作用户所属的用户组必须拥有目标终端组的权限
            boolean b = bindingRepository.batchCheckPermissions(operatorUgid, targetTgids).entrySet()
                    .stream()
                    .anyMatch(entry -> !entry.getValue());
            ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(b);
        }
    }

    @Override
    public void canViewUserGroupTerminalGroupBinding(Long oid, Long operatorUgid, Long targetUgid) {
        if (PermissionCheckSkipContext.isSkip()) {
            ExceptionEnum.ORG_PERMISSION_DENIED.throwIf(!userGroupRepository.ifTheSameOrg(oid, targetUgid));
        }
        // 操作用户层级校验：操作用户所属的用户组必须是目标用户组的上级用户组或同级用户组
        ExceptionEnum.USER_GROUP_PERMISSION_DENIED.throwIf(!userGroupRepository.isAncestorOrSibling(operatorUgid, targetUgid));
    }
}

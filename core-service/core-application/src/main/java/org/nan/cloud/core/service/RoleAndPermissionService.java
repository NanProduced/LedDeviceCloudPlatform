package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.UpdateRoleDTO;
import org.nan.cloud.core.domain.OperationPermission;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.domain.Role;
import org.nan.cloud.core.domain.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RoleAndPermissionService {

    Role createRole(Role role);

    Role getRoleByRid(Long rid);

    List<Role> getRolesByUid(Long uid);

    void updateRole(UpdateRoleDTO updateRoleDTO);

    void updateRoleOperationPermissionRel(Long rid, List<Long> operationPermissionIds);

    void deleteRole(Long oid, Long rid);

    void createRoleOperationPermissionRel(Long rid, Set<Long> operationPermissionIds);

    List<Permission> getPermissionsByIds(List<Long> permissionIds);

    List<Permission> getPermissionsByOperationPermissionIds(List<Long> operationPermissionIds);

    List<OperationPermission> getOperationPermissionByUid(Long uid);

    Set<Long> getOperationPermissionIdByUid(Long uid);

    /**
     * 组织管理员使用的获取全部权限接口
     * @return
     */
    List<Permission> getAllPermissions();

    List<OperationPermission> getAllOperations();

    Map<Long, List<Role>> getRolesByUids(List<Long> userIds);

    List<Role> getVisibleRolesByUid(Long oid, Long uid);

    List<Role> getAllRolesByOid(Long oid);

    void assignPermissionsToRole(Role role, List<Permission> permissions);

    void assignRoleToUser(Role role, User user);
}

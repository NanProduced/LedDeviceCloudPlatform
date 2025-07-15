package org.nan.cloud.core.service;

import org.nan.cloud.core.DTO.UpdateRoleDTO;
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

    void deleteRole(Long oid, Long rid);

    void createRolePermissionRel(Long rid, Set<Long> permissionIds);

    List<Permission> getPermissionsByIds(List<Long> permissionIds);

    List<Permission> getPermissionsByUid(Long oid, Long uid);

    List<Long> getPermissionIdsByRid(Long rid);

    /**
     * 组织管理员使用的获取全部权限接口
     * @return
     */
    List<Permission> getAllPermissions();

    Map<Long, List<Role>> getRolesByUids(List<Long> userIds);

    List<Role> getVisibleRolesByUid(Long oid, Long uid);

    void assignPermissionsToRole(Role role, List<Permission> permissions);

    void assignRoleToUser(Role role, User user);
}

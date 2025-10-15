package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Role;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RoleRepository {

    Role createRole(Role role);

    Role getRoleByRid(Long rid);

    Role getRoleDetailByRid(Long rid);

    List<Long> getRidsByUid(Long uid);

    List<Role> getRolesByUid(Long uid);

    void updateRole(Role role);

    void deleteRole(Long rid);

    boolean allRolesExist(List<Long> roles);

    List<Role> getRolesByRids(Collection<Long> rids);

    Map<Long, List<Long>> getRoleIdsByUserIds(List<Long> userIds);

    /**
     * 根据传入的角色Id查询权限覆盖的角色
     * @param rids
     * @param oid
     * @return
     */
    List<Role> getCoveredRolesByRids(Collection<Long> rids, Long oid);

    List<Role> getAllRolesByOid(Long oid);

    boolean ifTheSameOrg(Long oid, Long rid);

    boolean ifTheSameOrg(Long oid, List<Long> rids);

    /**
     * 获取组织中只有指定角色的用户
     * @param oid
     * @param rid
     * @return
     */
    List<Long> getUserWithOnlyRole(Long oid, Long rid);
}

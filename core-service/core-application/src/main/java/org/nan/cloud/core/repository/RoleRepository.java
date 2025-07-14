package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Role;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface RoleRepository {

    Role createRole(Role role);

    boolean allRolesExist(List<Long> roles);

    List<Role> getRolesByRids(Collection<Long> rids);

    Map<Long, List<Long>> getRoleIdsByUserIds(List<Long> userIds);
}

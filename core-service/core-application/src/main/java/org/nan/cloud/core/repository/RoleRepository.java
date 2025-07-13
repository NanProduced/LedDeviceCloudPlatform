package org.nan.cloud.core.repository;

import org.nan.cloud.core.domain.Role;

import java.util.List;

public interface RoleRepository {

    Role createRole(Role role);

    boolean allRolesExist(List<Long> roles);
}

package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.event.RemoveUserAndRoleRel;
import org.nan.cloud.core.event.RemoveUserAndRoleRelEvent;

import java.util.List;

public interface PermissionEventPublisher {

    void publishAddRoleAndPermissionRelEvent(Long rid, Long oid, List<Permission> permissions);

    void publishAddUserAndRoleRelEvent(Long uid, Long oid, List<Long> rids);

    void publishRemoveUserAndRoleRelEvent(RemoveUserAndRoleRel event);
}

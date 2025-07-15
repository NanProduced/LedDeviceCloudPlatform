package org.nan.cloud.core.service;

import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.event.rbac.RemoveUserAndRoleRel;

import java.util.List;

public interface PermissionEventPublisher {

    void publishAddRoleAndPermissionRelEvent(Long rid, Long oid, List<Permission> permissions);

    void publishAddUserAndRoleRelEvent(Long uid, Long oid, List<Long> rids);

    void publishRemoveUserAndRoleRelEvent(RemoveUserAndRoleRel event);

    void publishCoverUserAndRoleRelEvent(Long uid, Long oid, List<Long> rids);

    void publishChangeRoleAndPermissionRelEvent(Long rid, Long oid, List<Permission> permissions);

    void publishRemoveRoleEvent(Long rid, Long oid);
}

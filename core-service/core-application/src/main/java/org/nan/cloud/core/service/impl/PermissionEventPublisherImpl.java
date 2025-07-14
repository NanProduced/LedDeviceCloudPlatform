package org.nan.cloud.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.core.domain.Permission;
import org.nan.cloud.core.event.AddRoleAndPermissionRelEvent;
import org.nan.cloud.core.event.AddUserAndRoleRelEvent;
import org.nan.cloud.core.event.RemoveUserAndRoleRel;
import org.nan.cloud.core.event.RemoveUserAndRoleRelEvent;
import org.nan.cloud.core.service.PermissionEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PermissionEventPublisherImpl implements PermissionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishAddRoleAndPermissionRelEvent(Long rid, Long oid, List<Permission> permissions) {
        List<AddRoleAndPermissionRelEvent.UrlAndMethod> collect = permissions.stream().filter(Objects::nonNull)
                .map(e -> {
                    return new AddRoleAndPermissionRelEvent.UrlAndMethod(e.getUrl(), e.getMethod());
                }).toList();
        AddRoleAndPermissionRelEvent event = new AddRoleAndPermissionRelEvent(this, rid, oid, collect);
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publishAddUserAndRoleRelEvent(Long uid, Long oid, List<Long> rids) {
        applicationEventPublisher.publishEvent(new AddUserAndRoleRelEvent(this, uid, oid, rids));
    }

    @Override
    public void publishRemoveUserAndRoleRelEvent(RemoveUserAndRoleRel event) {
        applicationEventPublisher.publishEvent(new RemoveUserAndRoleRelEvent(this,
                event.getOidAndUid().getRight(),  event.getOidAndUid().getLeft()));
    }
}

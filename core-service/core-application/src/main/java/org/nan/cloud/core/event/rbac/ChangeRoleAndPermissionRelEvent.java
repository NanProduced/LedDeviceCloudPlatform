package org.nan.cloud.core.event.rbac;

import lombok.Getter;
import org.nan.cloud.core.DTO.UrlAndMethod;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class ChangeRoleAndPermissionRelEvent extends ApplicationEvent {

    @Getter
    private Long rid;

    @Getter
    private Long oid;

    @Getter
    private List<UrlAndMethod> permissions;

    public ChangeRoleAndPermissionRelEvent(Object source, Long oid, Long rid, List<UrlAndMethod> permissions) {
        super(source);
        this.oid = oid;
        this.rid = rid;
        this.permissions = permissions;
    }
}

package org.nan.cloud.core.event.rbac;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class RemoveRoleEvent extends ApplicationEvent {

    @Getter
    private Long rid;

    @Getter
    private Long oid;

    public RemoveRoleEvent(Object source, Long rid, Long oid) {
        super(source);
        this.rid = rid;
        this.oid = oid;
    }
}

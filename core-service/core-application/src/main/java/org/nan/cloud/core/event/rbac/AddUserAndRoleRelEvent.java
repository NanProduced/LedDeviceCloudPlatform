package org.nan.cloud.core.event.rbac;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class AddUserAndRoleRelEvent extends ApplicationEvent {

    @Getter
    private Long oid;

    @Getter
    private Long uid;

    @Getter
    private List<Long> rid;

    public AddUserAndRoleRelEvent(Object source,  Long uid, Long oid, List<Long> rid) {
        super(source);
        this.uid = uid;
        this.oid = oid;
        this.rid = rid;
    }
}

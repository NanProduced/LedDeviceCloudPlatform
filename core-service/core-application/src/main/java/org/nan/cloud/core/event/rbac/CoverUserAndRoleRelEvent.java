package org.nan.cloud.core.event.rbac;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class CoverUserAndRoleRelEvent extends ApplicationEvent {

    @Getter
    private Long oid;

    @Getter
    private Long uid;

    @Getter
    private List<Long> rid;

    public CoverUserAndRoleRelEvent(Object source,  Long oid, Long uid, List<Long> rid) {
        super(source);
        this.oid = oid;
        this.uid = uid;
        this.rid = rid;
    }
}

package org.nan.cloud.core.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class RemoveUserAndRoleRelEvent extends ApplicationEvent {

    @Getter
    private Long uid;

    @Getter
    private Long oid;

    public RemoveUserAndRoleRelEvent(Object source, Long uid, Long oid) {
        super(source);
        this.uid = uid;
        this.oid = oid;
    }
}

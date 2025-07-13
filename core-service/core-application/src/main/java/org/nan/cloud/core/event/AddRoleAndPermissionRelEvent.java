package org.nan.cloud.core.event;

import org.springframework.context.ApplicationEvent;

public class AddRoleAndPermissionRelEvent extends ApplicationEvent {

    public AddRoleAndPermissionRelEvent(Object source) {
        super(source);
    }
}

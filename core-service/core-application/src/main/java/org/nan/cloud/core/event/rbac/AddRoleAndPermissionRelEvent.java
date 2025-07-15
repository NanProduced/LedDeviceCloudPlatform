package org.nan.cloud.core.event.rbac;

import lombok.Getter;
import org.nan.cloud.core.DTO.UrlAndMethod;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class AddRoleAndPermissionRelEvent extends ApplicationEvent {

    @Getter
    private Long rid;

    @Getter
    private Long oid;

    @Getter
    private List<UrlAndMethod> urlAndMethods;

    public AddRoleAndPermissionRelEvent(Object source, Long rid, Long oid, List<UrlAndMethod> urlAndMethods) {
        super(source);
        this.rid = rid;
        this.oid = oid;
        this.urlAndMethods = urlAndMethods;
    }
}

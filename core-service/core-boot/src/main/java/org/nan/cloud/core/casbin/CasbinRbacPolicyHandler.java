package org.nan.cloud.core.casbin;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.event.*;
import org.nan.cloud.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CasbinRbacPolicyHandler {

    private final Enforcer rbacEnforcer;

    public CasbinRbacPolicyHandler(
            @Qualifier("rbacEnforcer") Enforcer rbacEnforcer) {
        this.rbacEnforcer = rbacEnforcer;
    }

    @EventListener
    public void onAddRoleAndPermissionRel(AddRoleAndPermissionRelEvent event) {
        Long rid = event.getRid();
        Long oid = event.getOid();
        for (AddRoleAndPermissionRelEvent.UrlAndMethod um :  event.getUrlAndMethods()) {
            boolean added = rbacEnforcer.addPolicy(
                    rid.toString(),
                    oid.toString(),
                    um.getUrl(),
                    um.getMethod()
            );
            if (!added) {
                log.warn("Casbin policy has exist，skip：rid={}, oid={}, url={}, method={}",
                        rid, oid, um.getUrl(), um.getMethod());
            }
        }

    }

    @EventListener
    public void onAddUserAndRoleRel(AddUserAndRoleRelEvent event) {
        Long uid = event.getUid();
        Long oid = event.getOid();
        event.getRid().forEach(role -> {
            if (!rbacEnforcer.addGroupingPolicy(uid.toString(),
                    role.toString(),
                    oid.toString())) throw new BusinessException(ExceptionEnum.CREATE_FAILED,
                    "A grouping policy already exists：(" + uid + ", " + role + ", " + oid + ")" );
        });
    }

    @EventListener
    public void onRemoveUserAndRoleRel(RemoveUserAndRoleRelEvent event) {
        rbacEnforcer.removeFilteredGroupingPolicy(0, event.getUid().toString(), "", event.getOid().toString());
    }

}

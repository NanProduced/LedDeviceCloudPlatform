package org.nan.cloud.core.casbin;

import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.DTO.UrlAndMethod;
import org.nan.cloud.core.event.rbac.*;
import org.nan.cloud.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class CasbinRbacPolicyHandler {

    private final Enforcer rbacEnforcer;

    public CasbinRbacPolicyHandler(
            @Qualifier("rbacEnforcer") Enforcer rbacEnforcer) {
        this.rbacEnforcer = rbacEnforcer;
    }

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAddRoleAndPermissionRel(AddRoleAndPermissionRelEvent event) {
        Long rid = event.getRid();
        Long oid = event.getOid();
        for (UrlAndMethod um :  event.getUrlAndMethods()) {
            boolean added = rbacEnforcer.addPolicy(
                    rid.toString(),
                    oid.toString(),
                    um.getUrl(),
                    um.getMethod(),
                    "allow"
            );
            if (!added) duplicatePolicyWarn(um, rid, oid);
        }

    }

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoveUserAndRoleRel(RemoveUserAndRoleRelEvent event) {
        rbacEnforcer.removeFilteredGroupingPolicy(0, event.getUid().toString(), "", event.getOid().toString());
    }

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCoverUserAndRoleRelEvent(CoverUserAndRoleRelEvent event) {
        Long uid = event.getUid();
        Long oid = event.getOid();
        rbacEnforcer.removeFilteredGroupingPolicy(0, uid.toString(), "", oid.toString());
        event.getRid().forEach(role -> {
            if (!rbacEnforcer.addGroupingPolicy(uid.toString(),
                    role.toString(),
                    oid.toString())) throw new BusinessException(ExceptionEnum.CREATE_FAILED,
                    "A grouping policy already exists：(" + uid + ", " + role + ", " + oid + ")" );
        });
    }

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChangeRoleAndPermissionRelEvent(ChangeRoleAndPermissionRelEvent event) {
        Long rid = event.getRid();
        Long oid = event.getOid();
        rbacEnforcer.removeFilteredNamedPolicy("p", 0, rid.toString(), oid.toString());
        for (UrlAndMethod um :  event.getPermissions()) {
            boolean added = rbacEnforcer.addPolicy(
                    rid.toString(),
                    oid.toString(),
                    um.getUrl(),
                    um.getMethod(),
                    "allow"
            );
            if (!added) duplicatePolicyWarn(um, rid, oid);
        }
    }

    @Async("casbinEventThreadPool")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRemoveRoleEvent(RemoveRoleEvent event) {
        rbacEnforcer.removeFilteredNamedPolicy("p", 0, event.getRid().toString(), event.getOid().toString());
        rbacEnforcer.removeFilteredGroupingPolicy(1, event.getRid().toString(), event.getOid().toString());
    }


    private void duplicatePolicyWarn(UrlAndMethod um, Long rid, Long oid) {
        log.warn("Casbin policy has exist，skip：rid={}, oid={}, url={}, method={}",
                rid, oid, um.getUrl(), um.getMethod());
    }

}

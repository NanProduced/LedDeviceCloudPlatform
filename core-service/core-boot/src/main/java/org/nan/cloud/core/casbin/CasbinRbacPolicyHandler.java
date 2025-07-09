package org.nan.cloud.core.casbin;

import org.casbin.jcasbin.main.Enforcer;
import org.nan.cloud.common.basic.exception.ExceptionEnum;
import org.nan.cloud.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CasbinRbacPolicyHandler {

    private final Enforcer rbacEnforcer;

    public CasbinRbacPolicyHandler(
            @Qualifier("rbacEnforcer") Enforcer rbacEnforcer) {
        this.rbacEnforcer = rbacEnforcer;
    }

    public boolean addRolePolicy(Long rid, Long oid, String url, String method) {
        boolean added = rbacEnforcer.addPolicy(
                rid.toString(),
                oid.toString(),
                url,
                method);
        return added;
    }

    public void addGroupPolicy(Long uid, Long rid, Long oid) {
        if (!rbacEnforcer.addGroupingPolicy(uid.toString(),
                rid.toString(),
                oid.toString())) throw new BusinessException(ExceptionEnum.CREATE_FAILED,
                "A grouping policy already exists：(" + uid + ", " + rid + ", " + oid + ")" );
    }
}

package org.nan.cloud.core.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.nan.cloud.core.manager.PermissionCheckSkipContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OrgManagerMarkAspect {

    @Pointcut("@annotation(SkipOrgManagerPermissionCheck) || @within(SkipOrgManagerPermissionCheck)")
    public void skipPointcut() {}

    @Around("skipPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean isOrgManager = InvocationContextHolder.ifOrgManager();
        if (isOrgManager) {
            PermissionCheckSkipContext.setSkip(true);
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (isOrgManager) {
                PermissionCheckSkipContext.clear();
            }
        }
    }
}

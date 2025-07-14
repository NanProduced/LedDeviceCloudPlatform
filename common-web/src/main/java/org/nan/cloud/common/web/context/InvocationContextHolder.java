package org.nan.cloud.common.web.context;

import org.nan.cloud.common.web.interceptor.InvocationContextExtractInterceptor;
import org.springframework.lang.NonNull;

/**
 * Servlet环境下当前请求用户上下文持有者,不支持子线程获取
 *
 * 通过 {@link InvocationContextExtractInterceptor} 拦截请求并放进ThreadLocal
 */
public class InvocationContextHolder {

    private static final ThreadLocal<GenericInvocationContext> contextHolder = new ThreadLocal<>();

    private InvocationContextHolder() {

    }

    public static void clearContext() {
        contextHolder.remove();
    }

    @NonNull
    public static GenericInvocationContext getContext() {
        GenericInvocationContext context = contextHolder.get();
        if (context == null) {
            return createEmptyContext();
        }
        return context;
    }

    public static void setContext(GenericInvocationContext context) {
        contextHolder.set(context);
    }

    @NonNull
    public static GenericInvocationContext createEmptyContext() {
        GenericInvocationContext context = new GenericInvocationContext();
        context.setRequestUser(new RequestUserInfo());
        contextHolder.set(context);
        return contextHolder.get();
    }

    @NonNull
    public static Long getUgid() {
        return contextHolder.get().getRequestUser().getUgid();
    }

    public static Long getOid() {return contextHolder.get().getRequestUser().getOid();}

    public static boolean ifOrgManager() {
        return contextHolder.get().getRequestUser().getUserType() == 1;
    }

    @NonNull
    public static Long getCurrentUId() {
        return contextHolder.get().getRequestUser().getUid();
    }
}

package org.nan.cloud.common.context;

import org.nan.cloud.common.web.InvocationContextExtractInterceptor;
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
        return contextHolder.get();
    }

    public static void setContext(GenericInvocationContext context) {
        contextHolder.set(context);
    }

    @NonNull
    public static GenericInvocationContext createEmptyContext() {
        contextHolder.set(new GenericInvocationContext());
        return contextHolder.get();
    }

    @NonNull
    public static Long getUgid() {
        return contextHolder.get().getRequestUser().getUgid();
    }

    @NonNull
    public static Long getCurrentUId() {
        return contextHolder.get().getRequestUser().getUid();
    }
}

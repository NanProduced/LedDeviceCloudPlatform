package org.nan.cloud.core.manager;

public class PermissionCheckSkipContext {

    private static final ThreadLocal<Boolean> SKIP = ThreadLocal.withInitial(() -> false);

    public static void setSkip(boolean skip) {
        SKIP.set(skip);
    }

    public static boolean isSkip() {
        return SKIP.get();
    }

    public static void clear() {
        SKIP.remove();
    }

}

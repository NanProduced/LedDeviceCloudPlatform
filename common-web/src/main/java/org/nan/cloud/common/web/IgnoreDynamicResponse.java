package org.nan.cloud.common.web;

import java.lang.annotation.*;

/**
 * 注解不需要统一包装返回值的方法
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface IgnoreDynamicResponse {
}

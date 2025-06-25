package org.nan.cloud.common.web;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;

import java.nio.charset.StandardCharsets;

public class FeignInvocationInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        GenericInvocationContext ctx = InvocationContextHolder.getContext();
        if (ctx.isUserRequest()) {
            String userJson = ctx.getRequestUserAsJson();
            String encoded = java.util.Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(userJson.getBytes(StandardCharsets.UTF_8));
            template.header(CustomHttpHeaders.REQUEST_USER, encoded);
        }
    }
}

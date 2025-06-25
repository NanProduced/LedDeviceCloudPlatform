package org.nan.cloud.common.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nan.cloud.common.web.context.GenericInvocationContext;
import org.nan.cloud.common.web.context.InvocationContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class InvocationContextExtractInterceptor implements WebRequestInterceptor {

    @Override
    public void preHandle(@NonNull WebRequest request) throws Exception {
        final ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        GenericInvocationContext context = new GenericInvocationContext();
        String requestHeader = servletWebRequest.getHeader(CustomHttpHeaders.REQUEST_USER);

        if (StringUtils.isNotBlank(requestHeader)) {
            byte[] decoded = Base64.getUrlDecoder().decode(requestHeader);
            String json = new String(decoded, StandardCharsets.UTF_8);
            context.setRequestUser(json);
        }

        InvocationContextHolder.setContext(context);

    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {

    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {
        GenericInvocationContext context = InvocationContextHolder.getContext();
        log.info("调用afterCompletion, context:{}", context);
        InvocationContextHolder.clearContext();
        log.info("afterCompletion清除上下文信息");
    }
}

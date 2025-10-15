package org.nan.cloud.common.web.config;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.common.web.interceptor.InvocationContextExtractInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InvocationContextExtractInterceptor invocationContextExtractInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加InvocationContextExtractInterceptor拦截器，用于提取用户上下文
        registry.addWebRequestInterceptor(invocationContextExtractInterceptor);
    }
} 
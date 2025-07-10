package org.nan.cloud.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.nan.cloud.common.basic.utils.JsonUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class BaseResponseAdvice implements ResponseBodyAdvice {

    private static final List<String> OPENAI_PATHS = Arrays.asList(
            "/v3/api-docs",        // OpenAPI 3 JSON/YAML 主路径
            "/v3/api-docs.yaml",
            "/v3/api-docs.json",
            "/swagger-ui",         // Swagger UI 根路径
            "/swagger-ui.html",    // Swagger UI HTML 页面
            "/webjars"             // Swagger UI 依赖的 webjars 资源
    );

    private static final List<String> STANDARD_FEIGN_HEADERS = Arrays.asList(
            "traceparent",
            "tracestate",
            "b3",
            "X-B3-TraceId",
            "X-B3-SpanId"
    );

    @Override
    public boolean supports(MethodParameter methodParameter, Class converterType) {

        // 忽略 @IgnoreDynamicResponse 注解
        if (methodParameter.hasMethodAnnotation(IgnoreDynamicResponse.class)) {
            return false;
        }

        // 忽略 openapi 相关请求
        HttpServletRequest servletRequest = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            servletRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        if (null != servletRequest) {
            String path = servletRequest.getRequestURI();
            for (String openApiPath : OPENAI_PATHS) {
                if (path.startsWith(openApiPath)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object obj, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        HttpHeaders headers = request.getHeaders();
        // 只要存在标准的 traceparent (W3C) 或 b3 (B3) 头，就认为是内部 Feign 调用
        if (STANDARD_FEIGN_HEADERS.stream().anyMatch(headers::containsKey)) {
            return obj;
        }

        // 已经包装过的响应不做处理
        if (obj instanceof DynamicResponse) {
            return obj;
        }

        //当返回类型是String时,消息转换器为StringHttpMessage,需先将统一响应体转为json
        if (obj instanceof String) {
            return JsonUtils.toJson(DynamicResponse.success(obj));
        }

        // 返回前端默认进行统一包装
        return DynamicResponse.success(obj);
    }
}

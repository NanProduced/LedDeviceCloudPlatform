package org.nan.cloud.common.web;

import org.nan.cloud.common.basic.utils.JsonUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class BaseResponseAdvice implements ResponseBodyAdvice {

    @Override
    public boolean supports(MethodParameter methodParameter, Class converterType) {
        return !methodParameter.hasMethodAnnotation(IgnoreDynamicResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object obj, MethodParameter returnType, MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        HttpHeaders headers = request.getHeaders();
        // 只要存在标准的 traceparent (W3C) 或 b3 (B3) 头，就认为是内部 Feign 调用
        boolean isFeignInvocation =
                headers.containsKey("traceparent") ||
                        headers.containsKey("tracestate") ||
                        headers.containsKey("b3") ||
                        headers.containsKey("X-B3-TraceId") ||
                        headers.containsKey("X-B3-SpanId");

        // Feign 调用返回不走外层包装
        if (isFeignInvocation) {
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

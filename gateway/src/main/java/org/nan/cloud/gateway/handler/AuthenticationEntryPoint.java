package org.nan.cloud.gateway.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义登录认证入口点，处理不同请求类型的认证失败响应
 * - Ajax请求 (Accept: application/json 或 X-Requested-With: XMLHttpRequest) 返回401 Unauthorized。
 * - OPTIONS请求 (跨域预检) 返回200 OK。
 * - 其他请求 (非Ajax、非OPTIONS) 执行OAuth2重定向登录。
 */
public class AuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    // OAuth2 登录端点 /oauth2/authorization/{registrationId}
    private final String oauth2LoginEndpoint;

    /**
     * 构造函数注入OAuth2登录端点URI。
     *  "{OAuth2 Authorization Server Host}/oauth2/authorization/{client-id}"
     * @param oauth2LoginEndpoint OAuth2登录重定向的URI
     */
    public AuthenticationEntryPoint(String oauth2LoginEndpoint) {
        this.oauth2LoginEndpoint = oauth2LoginEndpoint;
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        DelegatingServerAuthenticationEntryPoint delegatingEntryPoint = buildDelegatingEntryPoint();
        // 调用其commence方法处理请求
        return delegatingEntryPoint.commence(exchange, ex);
    }

    private DelegatingServerAuthenticationEntryPoint buildDelegatingEntryPoint() {
        // 请求头accept为application/json且忽略*/*
        // 用于过滤前端请求/Api接口请求（Postman）
        MediaTypeServerWebExchangeMatcher applicationJsonMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.APPLICATION_JSON);
        applicationJsonMatcher.setIgnoredMediaTypes(Stream.of(MediaType.ALL).collect(Collectors.toSet()));

        ServerWebExchangeMatcher contentTypeJsonMatcher = exchange -> {
            String ct = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            boolean match = StringUtils.hasText(ct)
                    && ct.contains(MediaType.APPLICATION_JSON_VALUE);
            return match ? ServerWebExchangeMatcher.MatchResult.match() : ServerWebExchangeMatcher.MatchResult.notMatch();
        };

        List<DelegatingServerAuthenticationEntryPoint.DelegateEntry> delegateEntryList = Arrays.asList(
                // 请求头accept为application/json -> 返回401
                new DelegatingServerAuthenticationEntryPoint.DelegateEntry(
                        applicationJsonMatcher,
                        new JsonRespAuthenticationEntryPoint()
                ),
                // 请求头Content-Type为application/json -> 返回401
                new DelegatingServerAuthenticationEntryPoint.DelegateEntry(
                        contentTypeJsonMatcher,
                        new JsonRespAuthenticationEntryPoint()
                ),
                // 请求头X-Requested-With为XMLHttpRequest -> 返回401
                // jQuery AJAX 请求
                new DelegatingServerAuthenticationEntryPoint.DelegateEntry(new ServerWebExchangeMatcher() {
                    @Override
                    public Mono<MatchResult> matches(ServerWebExchange exchange) {
                        String xRequestedWith = exchange.getRequest().getHeaders().getFirst("X-Requested-With");
                        boolean match = StringUtils.hasText(xRequestedWith) && xRequestedWith.equals("XMLHttpRequest");
                        return match ? MatchResult.match() : MatchResult.notMatch();
                    }
                }, new JsonRespAuthenticationEntryPoint()),
                // 跨域OPTIONS请求返回200（解决浏览器报错）
                new DelegatingServerAuthenticationEntryPoint.DelegateEntry(new ServerWebExchangeMatcher() {
                    @Override
                    public Mono<MatchResult> matches(ServerWebExchange exchange) {
                        HttpMethod method = exchange.getRequest().getMethod();
                        boolean match = HttpMethod.OPTIONS.equals(method);
                        return match ? MatchResult.match() : MatchResult.notMatch();
                    }
                }, new HttpStatusServerEntryPoint(HttpStatus.OK))
        );

        DelegatingServerAuthenticationEntryPoint nonAjaxLoginEntryPoint = new DelegatingServerAuthenticationEntryPoint(delegateEntryList);
        // 默认登录入口即为OAuth2重定向登录端点
        // 浏览器直接访问接口 未登录则跳到登录页
        nonAjaxLoginEntryPoint.setDefaultEntryPoint(new RedirectServerAuthenticationEntryPoint(this.oauth2LoginEndpoint));

        return nonAjaxLoginEntryPoint;
    }

}

package org.nan.cloud.auth.boot.login;

import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.nan.cloud.auth.infrastructure.security.ExtendedLoginUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.RequestCache;

/**
 * 拓展登录方式
 */
public class ExtendedLoginAuthenticationSecurityConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final ExtendedLoginUserService extendedLoginUserService;

    private final OAuth2ServerProps oAuth2ServerProps;

    private final RequestCache requestCache;

    public ExtendedLoginAuthenticationSecurityConfig(ExtendedLoginUserService extendedLoginUserService, OAuth2ServerProps oAuth2ServerProps, RequestCache requestCache) {
        this.extendedLoginUserService = extendedLoginUserService;
        this.oAuth2ServerProps = oAuth2ServerProps;
        this.requestCache = requestCache;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {

        ExtendedLoginAuthenticationProcessingFilter extendedLoginAuthenticationProcessingFilter = new ExtendedLoginAuthenticationProcessingFilter(
                oAuth2ServerProps.getLoginProcessingUrl(),
                http.getSharedObject(AuthenticationManager.class)
        );
        // 自定义登录成功、失败处理器
        extendedLoginAuthenticationProcessingFilter.setAuthenticationSuccessHandler(new ExtendedLoginJsonRespAuthenticationSuccessHandler(requestCache));
        extendedLoginAuthenticationProcessingFilter.setAuthenticationFailureHandler(new ExtendedLoginJsonRespAuthenticationFailureHandler());
        // 认证处理器
        ExtendedLoginAuthenticationProvider extendedLoginAuthenticationProvider = new ExtendedLoginAuthenticationProvider(extendedLoginUserService);

        http.authenticationProvider(extendedLoginAuthenticationProvider)
                .addFilterBefore(extendedLoginAuthenticationProcessingFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

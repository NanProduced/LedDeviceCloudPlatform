package org.nan.cloud.auth.boot.config;

import org.nan.cloud.auth.boot.login.ExtendedLoginAuthenticationSecurityConfig;
import org.nan.cloud.auth.boot.oauth.OidcAuthorizationService;
import org.nan.cloud.auth.infrastructure.security.DefaultExtendedLoginUserServiceImpl;
import org.nan.cloud.auth.infrastructure.security.ExtendedLoginUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {"/css/**", "/js/**", "/webjars/**", "/img/**", "/favicon.ico"};

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http,
                                              OAuth2ServerProps oAuth2ServerProps,
                                              ExtendedLoginUserService extendedLoginUserService,
                                              RequestCache requestCache) throws Exception {
        http
                .with(new ExtendedLoginAuthenticationSecurityConfig(
                        extendedLoginUserService,
                        oAuth2ServerProps,
                        requestCache
                ),configure -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/rsa/publicKey").permitAll()
                        .requestMatchers(oAuth2ServerProps.getLoginPageUrl()).permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers(AUTH_WHITELIST)
                    .ignoringRequestMatchers(new AntPathRequestMatcher("/login", "POST")))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .loginProcessingUrl("/login"));
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestCache requestCache() {
        return new HttpSessionRequestCache();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher(ApplicationEventPublisher delegate) {
        return new DefaultAuthenticationEventPublisher(delegate);
    }

    /**
     * 默认使用带 org suffix 的验证服务
     * @param userDetailsService
     * @param passwordEncoder
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public ExtendedLoginUserService extendedLoginUserService(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        return new DefaultExtendedLoginUserServiceImpl(userDetailsService, passwordEncoder);
    }
}

package org.nan.cloud.auth.boot.config;

import org.nan.cloud.auth.boot.oauth.OidcAuthorizationService;
import org.nan.cloud.auth.boot.oidc.OidcLogoutSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

@Configuration
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {"/css/**", "/js/**", "/webjars/**", "/img/**", "/favicon.ico"};

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http, RegisteredClientRepository registeredClientRepository, OidcAuthorizationService oidcAuthorizationService, OAuth2ServerProps oAuth2ServerProps) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/rsa/publicKey").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers(AUTH_WHITELIST))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", false)
                        .loginProcessingUrl("/login"))
                .logout(logout -> logout
                        .logoutUrl("/connect/logout")
                        .logoutSuccessHandler(new OidcLogoutSuccessHandler(registeredClientRepository, oidcAuthorizationService, oAuth2ServerProps))
                );

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
}

package org.nan.cloud.terminal.config.security;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.terminal.config.security.auth.TerminalAuthenticationProvider;
import org.nan.cloud.terminal.config.security.filter.TerminalAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Terminal Service安全配置
 * 
 * 针对终端设备每次请求都携带Basic Auth的特点进行优化：
 * 1. 完全无状态设计 - 不创建任何会话
 * 2. 高性能认证 - Redis缓存认证结果，减少数据库查询
 * 3. 专用认证提供者 - 针对设备认证场景优化
 * 4. 智能缓存策略 - 动态TTL，活跃设备缓存更久
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class TerminalSecurityConfig {

    // 移除字段注入，改用方法参数注入避免循环依赖

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10); // 适中的强度，平衡安全性和性能
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, 
                                                     TerminalAuthenticationProvider authProvider) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = 
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider);
        return authenticationManagerBuilder.build();
    }

    /**
     * Basic Auth入口点
     */
    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("Terminal Service");
        return entryPoint;
    }

    /**
     * 终端认证过滤器
     */
    @Bean
    public TerminalAuthenticationFilter terminalAuthenticationFilter(
            AuthenticationManager authenticationManager) {
        return new TerminalAuthenticationFilter(authenticationManager);
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, 
                                         AuthenticationManager authenticationManager,
                                         TerminalAuthenticationFilter terminalAuthenticationFilter) throws Exception {
        
        http
            // 禁用CSRF - 终端设备API无需CSRF保护
            .csrf(AbstractHttpConfigurer::disable)
            
            // 禁用CORS - 根据需要配置
            .cors(AbstractHttpConfigurer::disable)
            
            // 完全无状态 - 不创建任何会话
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // HTTP安全头配置 - 兼容Spring Security 6.1+
            .headers(headers -> headers
                // 防止页面被嵌入iframe，避免点击劫持攻击
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                // 启用MIME类型嗅探保护，防止MIME类型混淆攻击
                .contentTypeOptions(Customizer.withDefaults())
                // 配置HSTS，强制HTTPS连接
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)  // 1年
                    .includeSubDomains(true))
                // 禁用缓存敏感页面
                .cacheControl(Customizer.withDefaults())
                // 添加自定义安全头
                .addHeaderWriter((request, response) -> {
                    response.setHeader("X-XSS-Protection", "1; mode=block");
                    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                })
            )
            
            // 请求授权配置
            .authorizeHttpRequests(authz -> authz
                // 健康检查端点无需认证
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Prometheus监控端点无需认证(可根据需要调整)
                .requestMatchers("/actuator/prometheus").permitAll()
                
                // WebSocket端点使用自定义认证
                .requestMatchers("/terminal/ws/**").permitAll()
                
                // 设备API端点需要Basic Auth认证
                .requestMatchers("/wp-json/**").authenticated()
                .requestMatchers("/api/terminal/**").authenticated()
                
                // 其他所有请求需要认证
                .anyRequest().authenticated()
            )
            
            // 使用自定义认证过滤器
            .addFilterBefore(terminalAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Basic Auth配置
            .httpBasic(basic -> basic
                .authenticationEntryPoint(basicAuthenticationEntryPoint())
            );

        return http.build();
    }
}
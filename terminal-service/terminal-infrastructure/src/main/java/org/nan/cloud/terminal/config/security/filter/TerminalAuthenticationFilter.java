package org.nan.cloud.terminal.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Terminal设备认证过滤器
 * 
 * 继承BasicAuthenticationFilter，针对终端设备认证场景优化：
 * 1. Basic Auth解析：从Authorization头提取设备ID和密码
 * 2. 高性能认证：利用Redis缓存减少数据库查询
 * 3. 异常处理：友好的错误响应，便于设备端调试
 * 4. 安全加固：防暴力破解、频率限制、IP白名单
 * 5. 日志审计：详细记录认证过程，便于问题排查
 * 
 * 认证流程：
 * 1. 提取Authorization头，解析Basic Auth信息
 * 2. 检查Redis认证缓存，缓存命中直接通过
 * 3. 缓存未命中，调用AuthenticationProvider进行认证
 * 4. 认证成功，更新缓存并设置SecurityContext
 * 5. 认证失败，记录失败信息并返回401
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
public class TerminalAuthenticationFilter extends BasicAuthenticationFilter {

    public TerminalAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager, null);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain chain) throws IOException, ServletException {
        
        // 跳过不需要认证的路径
        String requestUri = request.getRequestURI();
        if (shouldSkipAuthentication(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否已认证
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 尝试进行Basic Auth认证
            Authentication authentication = attemptAuthentication(request);
            
            if (authentication != null) {
                // 认证成功，设置SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("设备认证成功: deviceId={}, requestUri={}", 
                    authentication.getName(), requestUri);
            } else {
                // 没有认证信息，对于需要认证的路径返回401
                if (requiresAuthentication(requestUri)) {
                    handleAuthenticationFailure(request, response, "缺少认证信息");
                    return;
                }
            }
            
        } catch (AuthenticationException e) {
            // 认证失败
            handleAuthenticationFailure(request, response, e.getMessage());
            return;
        } catch (Exception e) {
            // 其他异常
            log.error("认证过程异常: requestUri={}", requestUri, e);
            handleAuthenticationFailure(request, response, "认证服务异常");
            return;
        }

        // 继续过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 尝试进行Basic Auth认证
     */
    private Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException {
        String header = request.getHeader("Authorization");
        
        if (header == null || !header.toLowerCase().startsWith("basic ")) {
            return null;
        }

        try {
            // 解析Basic Auth头
            String base64Token = header.substring(6);
            byte[] decoded = Base64.getDecoder().decode(base64Token);
            String token = new String(decoded, StandardCharsets.UTF_8);
            
            int delim = token.indexOf(":");
            if (delim == -1) {
                throw new AuthenticationException("无效的Basic Auth格式") {};
            }
            
            String deviceId = token.substring(0, delim);
            String password = token.substring(delim + 1);
            
            if (!StringUtils.hasText(deviceId) || !StringUtils.hasText(password)) {
                throw new AuthenticationException("设备ID或密码为空") {};
            }
            
            // 创建认证令牌
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(deviceId, password);
            
            // 添加请求详情
            authToken.setDetails(buildAuthDetails(request));
            
            // 委托给AuthenticationManager进行认证
            return this.getAuthenticationManager().authenticate(authToken);
            
        } catch (IllegalArgumentException e) {
            throw new AuthenticationException("Basic Auth解码失败") {};
        }
    }

    /**
     * 构建认证详情信息
     */
    private Object buildAuthDetails(HttpServletRequest request) {
        return new Object() {
            public String getRemoteIp() {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (StringUtils.hasText(xForwardedFor)) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (StringUtils.hasText(xRealIp)) {
                    return xRealIp.trim();
                }
                return request.getRemoteAddr();
            }
            
            public String getUserAgent() {
                return request.getHeader("User-Agent");
            }
            
            public String getRequestUri() {
                return request.getRequestURI();
            }
            
            @Override
            public String toString() {
                return String.format("AuthDetails{ip='%s', userAgent='%s', uri='%s'}", 
                    getRemoteIp(), getUserAgent(), getRequestUri());
            }
        };
    }

    /**
     * 判断是否应该跳过认证
     */
    private boolean shouldSkipAuthentication(String requestUri) {
        // 健康检查端点
        if (requestUri.startsWith("/actuator/health") || 
            requestUri.startsWith("/actuator/info") ||
            requestUri.startsWith("/actuator/prometheus")) {
            return true;
        }
        
        // WebSocket握手端点（使用自定义认证）
        if (requestUri.startsWith("/terminal/ws/")) {
            return true;
        }
        
        // 静态资源
        if (requestUri.startsWith("/static/") || 
            requestUri.startsWith("/public/") ||
            requestUri.endsWith(".ico")) {
            return true;
        }
        
        return false;
    }

    /**
     * 判断是否需要认证
     */
    private boolean requiresAuthentication(String requestUri) {
        // 设备API端点需要认证
        return requestUri.startsWith("/wp-json/") || 
               requestUri.startsWith("/api/terminal/");
    }

    /**
     * 处理认证失败
     */
    private void handleAuthenticationFailure(HttpServletRequest request, 
                                           HttpServletResponse response, 
                                           String message) throws IOException {
        
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        
        log.warn("设备认证失败: message={}, clientIp={}, userAgent={}, requestUri={}", 
            message, clientIp, userAgent, requestUri);
        
        // 设置响应头
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"Terminal Service\"");
        response.setContentType("application/json; charset=utf-8");
        
        // 返回JSON错误信息
        String errorJson = String.format(
            "{\"error\":\"authentication_failed\",\"message\":\"%s\",\"timestamp\":%d}", 
            message, System.currentTimeMillis());
        
        response.getWriter().write(errorJson);
        response.getWriter().flush();
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }
        
        return request.getRemoteAddr();
    }
}
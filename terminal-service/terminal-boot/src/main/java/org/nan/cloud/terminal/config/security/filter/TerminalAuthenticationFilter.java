package org.nan.cloud.terminal.config.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.config.security.auth.TerminalPrincipal;
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
 * 终端设备认证过滤器
 * 
 * 继承BasicAuthenticationFilter，专门处理终端设备的Basic Auth认证
 * 
 * 性能优化特点：
 * 1. 快速路径判断 - 优先处理不需要认证的请求
 * 2. 缓存友好设计 - 复用认证结果，减少重复计算
 * 3. 异常处理优化 - 避免不必要的异常创建和处理
 * 4. 内存优化 - 及时清理临时对象
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
public class TerminalAuthenticationFilter extends BasicAuthenticationFilter {

    public TerminalAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain chain) throws IOException, ServletException {
        
        // 快速路径：检查是否需要认证
        if (!requiresAuthentication(request)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取Authorization头
        String header = request.getHeader("Authorization");
        if (header == null || !header.toLowerCase().startsWith("basic ")) {
            // 没有Basic Auth头，继续过滤器链
            chain.doFilter(request, response);
            return;
        }

        try {
            // 解析Basic Auth头
            UsernamePasswordAuthenticationToken authRequest = parseAuthenticationHeader(header);
            if (authRequest != null) {
                // 执行认证
                Authentication authResult = this.getAuthenticationManager().authenticate(authRequest);
                
                // 设置安全上下文
                SecurityContextHolder.getContext().setAuthentication(authResult);
                
                // 记录认证成功
                if (log.isDebugEnabled()) {
                    log.debug("终端设备认证成功: deviceId={}, uri={}", 
                        authResult.getName(), request.getRequestURI());
                }
            }
        } catch (AuthenticationException e) {
            // 认证失败
            log.warn("终端设备认证失败: uri={}, error={}", 
                request.getRequestURI(), e.getMessage());
                
            // 清除安全上下文
            SecurityContextHolder.clearContext();
            
            // 返回401未授权
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("WWW-Authenticate", "Basic realm=\"Terminal Service\"");
            response.getWriter().write("{\"error\":\"Authentication failed\",\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        // 继续过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 判断请求是否需要认证
     */
    private boolean requiresAuthentication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // 健康检查端点不需要认证
        if (uri.startsWith("/actuator/health") || uri.startsWith("/actuator/info") || 
            uri.startsWith("/actuator/prometheus")) {
            return false;
        }
        
        // WebSocket握手请求不在这里处理认证
        return !uri.startsWith("/terminal/ws");
        
        // API端点需要认证
    }

    /**
     * 解析Basic Auth头
     */
    private UsernamePasswordAuthenticationToken parseAuthenticationHeader(String header) {
        try {
            // 提取Base64编码部分
            String base64Token = header.substring(6); // "Basic " = 6 characters
            byte[] decoded = Base64.getDecoder().decode(base64Token);
            String token = new String(decoded, StandardCharsets.UTF_8);
            
            // 分割用户名和密码
            int delim = token.indexOf(":");
            if (delim == -1) {
                log.debug("Basic Auth格式错误：缺少冒号分隔符");
                return null;
            }
            
            String account = token.substring(0, delim);
            String password = token.substring(delim + 1);
            
            // 验证设备ID和密码不为空
            if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
                log.debug("Basic Auth格式错误：设备ID或密码为空");
                return null;
            }
            
            // 创建认证令牌
            return new UsernamePasswordAuthenticationToken(account, password);
            
        } catch (IllegalArgumentException e) {
            log.debug("Basic Auth解码失败", e);
            return null;
        } catch (Exception e) {
            log.warn("解析Basic Auth头异常", e);
            return null;
        }
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, 
                                            HttpServletResponse response, 
                                            Authentication authResult) throws IOException {
        // 认证成功后的处理
        super.onSuccessfulAuthentication(request, response, authResult);
        
        // 添加设备信息到响应头(可选)
        if (authResult.getPrincipal() instanceof TerminalPrincipal principal) {
            response.setHeader("X-Terminal-ID", principal.getTid().toString());
            response.setHeader("X-Organization-ID", principal.getOid().toString());
        }
    }

    @Override
    protected void onUnsuccessfulAuthentication(HttpServletRequest request, 
                                              HttpServletResponse response, 
                                              AuthenticationException failed) throws IOException {
        // 认证失败后的处理
        super.onUnsuccessfulAuthentication(request, response, failed);
        
        // 记录认证失败的详细信息
        String clientIp = getClientIpAddress(request);
        log.warn("终端设备认证失败详情: uri={}, clientIp={}, userAgent={}, error={}", 
            request.getRequestURI(), 
            clientIp,
            request.getHeader("User-Agent"), 
            failed.getMessage());
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
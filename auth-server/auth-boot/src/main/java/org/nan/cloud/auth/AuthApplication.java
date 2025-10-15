package org.nan.cloud.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 认证服务启动类
 * 
 * 功能说明：
 * - OAuth2授权服务器，支持标准OAuth2/OIDC协议
 * - 用户身份认证和授权管理
 * - JWT令牌签发、验证和刷新
 * - 多种授权模式支持（授权码、PKCE、客户端模式）
 * - 用户会话管理和单点登录
 * - 客户端应用注册和管理
 * - 权限范围控制和资源保护
 * 
 * 服务端口：8081
 * 
 * 核心特性：
 * - 基于Spring Authorization Server实现
 * - 支持自定义登录页面和用户同意页面
 * - JWT令牌自动轮转和撤销
 * - 集成Spring Security进行安全防护
 * - 支持多种客户端认证方式
 * - 实时会话监控和管理
 * - CORS跨域和CSRF防护
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "org.nan.cloud")
@EnableDiscoveryClient
public class AuthApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 认证服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("可用处理器核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("最大堆内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        try {
            SpringApplication.run(AuthApplication.class, args);
            
            log.info("🔐 认证服务 (Authentication Server) 启动成功! 端口: 8081");
            log.info("🔑 OAuth2授权端点: http://localhost:8081/oauth2/authorize");
            log.info("🎫 令牌端点: http://localhost:8081/oauth2/token");
            log.info("🔍 令牌检查端点: http://localhost:8081/oauth2/introspect");
            log.info("❌ 令牌撤销端点: http://localhost:8081/oauth2/revoke");
            log.info("📋 OIDC配置: http://localhost:8081/.well-known/openid_configuration");
            log.info("🔐 用户登录页面: http://localhost:8081/login");
            log.info("👤 用户信息端点: http://localhost:8081/userinfo");
            log.info("📋 管理端点: http://localhost:8081/actuator");
            log.info("📊 健康检查: http://localhost:8081/actuator/health");
            log.info("========== 认证服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 认证服务启动失败！", e);
            System.exit(1);
        }
    }
}

package org.nan.cloud.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 网关服务启动类
 * 
 * 功能说明：
 * - 统一API入口和路由管理
 * - OAuth2客户端，处理前端认证流程
 * - 基于Casbin的细粒度权限验证
 * - 请求过滤、限流和安全防护
 * - 跨域请求处理和CORS配置
 * - 负载均衡和故障转移
 * - 请求响应日志记录和监控
 * 
 * 服务端口：8082
 * 
 * 核心特性：
 * - 基于Spring Cloud Gateway实现
 * - 支持Cookie和Bearer Token两种认证方式
 * - 集成Spring Security OAuth2 Client
 * - 动态路由配置和服务发现
 * - 全局异常处理和错误页面
 * - 请求追踪和链路监控
 * - 接口访问权限控制
 * - 高可用和容错机制
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 网关服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("可用处理器核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("最大堆内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        try {
            SpringApplication.run(GatewayApplication.class);
            
            log.info("🌐 网关服务 (API Gateway) 启动成功! 端口: 8082");
            log.info("🔀 API网关入口: http://localhost:8082");
            log.info("🔐 登录页面: http://localhost:8082/login");
            log.info("🔓 注销端点: http://localhost:8082/logout");
            log.info("👤 用户信息: http://localhost:8082/user");
            log.info("🏠 首页入口: http://localhost:8082/");
            log.info("🔑 OAuth2回调: http://localhost:8082/login/oauth2/code/auth-server");
            log.info("📋 管理端点: http://localhost:8082/actuator");
            log.info("📊 健康检查: http://localhost:8082/actuator/health");
            log.info("========== 网关服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 网关服务启动失败！", e);
            System.exit(1);
        }
    }
}

package org.nan.cloud.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 核心业务服务启动类
 * 
 * 功能说明：
 * - LED设备管理和控制中心
 * - 用户和组织架构管理
 * - RBAC权限控制和策略管理
 * - 业务数据处理和存储
 * - 设备状态监控和管理
 * - 用户行为日志和审计
 * - 业务流程编排和自动化
 * 
 * 服务端口：动态分配
 * 
 * 核心特性：
 * - 基于DDD分层架构设计
 * - 集成MyBatis Plus进行数据持久化
 * - 支持多租户和组织隔离
 * - 实时设备状态同步和监控
 * - 完整的审计日志和操作记录
 * - 灵活的权限配置和策略引擎
 * - 高性能缓存和数据查询优化
 * - 微服务间通信和协调
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "org.nan.cloud")
@EnableFeignClients(basePackages = {
    "org.nan.cloud.auth.api.client",
    "org.nan.cloud.terminal.api.feign"
})
public class CoreServiceApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 核心业务服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("可用处理器核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("最大堆内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        try {
            SpringApplication.run(CoreServiceApplication.class);
            
            log.info("🏢 核心业务服务 (Core Service) 启动成功!");
            log.info("========== 核心业务服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 核心业务服务启动失败！", e);
            System.exit(1);
        }
    }
}

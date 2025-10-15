package org.nan.cloud.terminal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LED设备终端通信服务启动类
 * 
 * 功能说明：
 * - 高性能WebSocket服务器，支持10K并发连接
 * - 基于Netty的设备终端通信协议
 * - 双端口架构：HTTP 8085 + WebSocket 8843
 * - 分片式连接管理(16个分片)，降低锁竞争
 * - 独立Basic Auth认证体系
 * - 实时设备指令下发和状态同步
 * - 设备心跳监控和连接管理
 * 
 * 服务端口：HTTP 8085 + WebSocket 8843
 * 
 * 核心特性：
 * - 基于Netty实现高性能WebSocket服务器
 * - 支持1万并发WebSocket连接
 * - 分片式连接存储，减少93.75%锁竞争
 * - WebSocket路径：/ColorWebSocket/websocket/chat
 * - URL参数认证：?username=xxx&password=xxx
 * - 三层数据存储：MySQL + MongoDB + Redis
 * - 55秒心跳机制，实时连接状态监控
 * - 系统调优：JVM G1GC + Linux网络参数优化
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "org.nan.cloud")
@EnableDiscoveryClient
@EnableScheduling
@EnableConfigurationProperties
public class TerminalServiceApplication {

    public static void main(String[] args) {
        // 添加启动前的系统信息
        log.info("========== 设备终端通信服务启动开始 ==========");
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("操作系统: {}", System.getProperty("os.name"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("可用处理器核心数: {}", Runtime.getRuntime().availableProcessors());
        log.info("最大堆内存: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        
        try {
            SpringApplication.run(TerminalServiceApplication.class, args);
            
            log.info("📡 设备终端通信服务 (Terminal Service) 启动成功!");
            log.info("🌐 HTTP服务端口: 8085");
            log.info("🔌 WebSocket服务端口: 8843");
            log.info("📱 设备HTTP接口: http://localhost:8085/terminal/**");
            log.info("🔗 WebSocket连接: ws://localhost:8843/ColorWebSocket/websocket/chat");
            log.info("🔐 认证方式: URL参数认证 (?username=xxx&password=xxx)");
            log.info("📋 管理端点: http://localhost:8085/actuator");
            log.info("📊 健康检查: http://localhost:8085/actuator/health");
            log.info("========== 设备终端通信服务启动完成 ==========");
            
        } catch (Exception e) {
            log.error("❌ 设备终端通信服务启动失败！", e);
            System.exit(1);
        }
    }
}
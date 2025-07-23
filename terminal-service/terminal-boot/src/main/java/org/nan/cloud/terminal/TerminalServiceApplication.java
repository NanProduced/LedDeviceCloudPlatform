package org.nan.cloud.terminal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * LED设备终端通信服务启动类
 * 
 * <p>支持1万并发WebSocket连接的高性能设备通信服务</p>
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>双协议支持：HTTP轮询 + WebSocket长连接</li>
 *   <li>高性能连接管理：分片式连接池，支持1万并发</li>
 *   <li>独立认证体系：Basic Auth，完全无状态设计</li>
 *   <li>三层数据存储：MySQL + MongoDB + Redis</li>
 *   <li>实时指令下发：55秒心跳，主动推送</li>
 * </ul>
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class TerminalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TerminalServiceApplication.class, args);
    }
}
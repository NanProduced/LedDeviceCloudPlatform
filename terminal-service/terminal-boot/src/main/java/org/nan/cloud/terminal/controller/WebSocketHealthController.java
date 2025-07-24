package org.nan.cloud.terminal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.websocket.handler.TerminalWebSocketHandler;
import org.nan.cloud.terminal.websocket.manager.ConnectionManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket健康检查控制器
 * 
 * 提供WebSocket服务的健康状态和性能指标查询：
 * 1. 连接统计：当前连接数、在线设备数、连接分布
 * 2. 性能指标：消息吞吐量、平均延迟、异常统计
 * 3. 系统状态：内存使用、Redis连接、数据库状态
 * 4. 服务可用性：服务启动时间、版本信息
 * 
 * 用于：
 * - Spring Boot Actuator健康检查集成
 * - 监控系统指标采集（Prometheus）
 * - 运维人员手动检查服务状态
 * - 负载均衡器健康检查端点
 * 
 * @author terminal-service  
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/terminal/health")
@RequiredArgsConstructor
public class WebSocketHealthController {

    private final ConnectionManager connectionManager;
    private final TerminalWebSocketHandler webSocketHandler;
    private final StringRedisTemplate redisTemplate;
    
    // 服务启动时间
    private final long serviceStartTime = System.currentTimeMillis();

    /**
     * 基础健康检查
     * 用于负载均衡器和监控系统的快速检查
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 基础状态信息
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("service", "terminal-websocket-service");
            health.put("version", "1.0.0");
            
            // 连接统计
            health.put("connections", connectionManager.getConnectionCount());
            health.put("onlineDevices", connectionManager.getOnlineDeviceCount());
            
            // 服务运行时间
            long uptime = System.currentTimeMillis() - serviceStartTime;
            health.put("uptimeMs", uptime);
            health.put("uptimeHours", uptime / (1000 * 60 * 60));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("健康检查异常", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 详细健康状态
     * 包含完整的系统指标和诊断信息
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 基础信息
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("serviceStartTime", serviceStartTime);
            health.put("uptime", System.currentTimeMillis() - serviceStartTime);
            
            // WebSocket连接详情
            Map<String, Object> websocket = new HashMap<>();
            websocket.put("totalConnections", connectionManager.getConnectionCount());
            websocket.put("onlineDevices", connectionManager.getOnlineDeviceCount());
            websocket.put("connectionStats", connectionManager.getConnectionStats());
            websocket.put("performanceStats", webSocketHandler.getPerformanceStats());
            health.put("websocket", websocket);
            
            // 系统资源状态
            Map<String, Object> system = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            system.put("availableProcessors", runtime.availableProcessors());
            system.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
            system.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
            system.put("usedMemoryMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            system.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
            health.put("system", system);
            
            // Redis连接状态
            Map<String, Object> redis = new HashMap<>();
            try {
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                redis.put("status", "PONG".equals(pong) ? "UP" : "DOWN");
                redis.put("response", pong);
            } catch (Exception e) {
                redis.put("status", "DOWN");
                redis.put("error", e.getMessage());
            }
            health.put("redis", redis);
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("详细健康检查异常", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 连接统计信息
     * 提供当前所有连接的详细统计
     */
    @GetMapping("/connections")
    public ResponseEntity<Map<String, Object>> connectionStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 基础统计
            stats.put("totalConnections", connectionManager.getConnectionCount());
            stats.put("onlineDevices", connectionManager.getOnlineDeviceCount());
            stats.put("timestamp", System.currentTimeMillis());
            
            // 在线设备列表
            List<String> onlineDevices = connectionManager.getAllOnlineDevices();
            stats.put("onlineDeviceList", onlineDevices);
            
            // 分片统计
            stats.put("connectionManagerStats", connectionManager.getConnectionStats());
            
            // 性能指标
            stats.put("performanceStats", webSocketHandler.getPerformanceStats());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("获取连接统计异常", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "获取连接统计失败",
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    /**
     * 性能指标
     * Prometheus格式的指标输出
     */
    @GetMapping("/metrics")
    public ResponseEntity<String> metrics() {
        try {
            StringBuilder metrics = new StringBuilder();
            
            // WebSocket连接指标
            metrics.append("# HELP terminal_websocket_connections Current WebSocket connections\n");
            metrics.append("# TYPE terminal_websocket_connections gauge\n");
            metrics.append("terminal_websocket_connections ").append(connectionManager.getConnectionCount()).append("\n");
            
            // 在线设备指标
            metrics.append("# HELP terminal_online_devices Current online devices\n");
            metrics.append("# TYPE terminal_online_devices gauge\n");
            metrics.append("terminal_online_devices ").append(connectionManager.getOnlineDeviceCount()).append("\n");
            
            // 服务运行时间
            metrics.append("# HELP terminal_service_uptime_seconds Service uptime in seconds\n");
            metrics.append("# TYPE terminal_service_uptime_seconds counter\n");
            metrics.append("terminal_service_uptime_seconds ").append((System.currentTimeMillis() - serviceStartTime) / 1000).append("\n");
            
            // JVM内存指标
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            metrics.append("# HELP terminal_jvm_memory_used_bytes Used JVM memory in bytes\n");
            metrics.append("# TYPE terminal_jvm_memory_used_bytes gauge\n");
            metrics.append("terminal_jvm_memory_used_bytes ").append(usedMemory).append("\n");
            
            metrics.append("# HELP terminal_jvm_memory_max_bytes Max JVM memory in bytes\n");
            metrics.append("# TYPE terminal_jvm_memory_max_bytes gauge\n");
            metrics.append("terminal_jvm_memory_max_bytes ").append(runtime.maxMemory()).append("\n");
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                .body(metrics.toString());
            
        } catch (Exception e) {
            log.error("获取性能指标异常", e);
            return ResponseEntity.status(500).body("# Error getting metrics: " + e.getMessage());
        }
    }

    /**
     * 清理超时连接
     * 手动触发连接清理，用于运维操作
     */
    @GetMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupConnections() {
        try {
            // 清理55秒超时的连接
            int cleanedCount = connectionManager.cleanupTimeoutConnections(55000);
            
            Map<String, Object> result = new HashMap<>();
            result.put("cleanedConnections", cleanedCount);
            result.put("remainingConnections", connectionManager.getConnectionCount());
            result.put("onlineDevices", connectionManager.getOnlineDeviceCount());
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("手动清理超时连接完成: 清理数量={}, 剩余连接数={}", 
                cleanedCount, connectionManager.getConnectionCount());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("清理连接异常", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "清理连接失败", 
                "message", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }
}
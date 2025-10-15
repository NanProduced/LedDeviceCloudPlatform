package org.nan.cloud.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 消息服务线程池配置
 * 
 * 提供消息处理相关的异步线程池
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 消息处理器线程池
     * 
     * 用于异步处理消息持久化、状态更新等任务
     * 配置参数：
     * - 核心线程数：8个
     * - 最大线程数：16个
     * - 队列容量：200个任务
     * - 拒绝策略：调用者运行策略，确保任务不丢失
     */
    @Bean("messageExecutor")
    public ThreadPoolTaskExecutor messageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("message-executor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * WebSocket消息分发线程池
     * 
     * 用于异步分发STOMP消息到不同的目标
     * 配置参数：
     * - 核心线程数：4个
     * - 最大线程数：12个
     * - 队列容量：100个任务
     * - 拒绝策略：丢弃最旧的任务策略，优先处理最新消息
     */
    @Bean("stompDispatchExecutor")
    public ThreadPoolTaskExecutor stompDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(12);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("stomp-dispatch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false); // WebSocket分发任务可以被打断
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 消息路由处理线程池
     * 
     * 用于异步处理消息路由决策和主题订阅管理
     * 配置参数：
     * - 核心线程数：2个
     * - 最大线程数：6个
     * - 队列容量：50个任务
     * - 拒绝策略：调用者运行策略，确保路由任务不丢失
     */
    @Bean("messageRoutingExecutor")
    public ThreadPoolTaskExecutor messageRoutingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setThreadNamePrefix("message-routing-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
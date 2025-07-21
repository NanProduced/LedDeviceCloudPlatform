package org.nan.cloud.message.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 * 
 * 为消息中心的异步任务提供线程池配置，包括用户上线推送、消息发送、
 * 任务结果通知等异步操作。针对LED设备云平台的业务特点进行优化配置。
 * 
 * 线程池策略：
 * - 消息任务执行器：处理用户上线推送、消息发送等轻量级任务
 * - 数据处理执行器：处理批量数据查询、统计分析等重量级任务
 * - 通知任务执行器：处理邮件、短信等外部通知任务
 * 
 * 性能特点：
 * - 核心线程保持活跃，减少任务等待时间
 * - 队列容量适中，防止内存溢出
 * - 拒绝策略友好，记录日志便于监控
 * - 线程命名规范，便于问题排查
 * 
 * @author Nan
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncTaskConfig {
    
    @Value("${message.async.message-task.core-pool-size:5}")
    private int messageTaskCorePoolSize;
    
    @Value("${message.async.message-task.max-pool-size:20}")
    private int messageTaskMaxPoolSize;
    
    @Value("${message.async.message-task.queue-capacity:200}")
    private int messageTaskQueueCapacity;
    
    @Value("${message.async.message-task.keep-alive-seconds:300}")
    private int messageTaskKeepAliveSeconds;
    
    @Value("${message.async.data-task.core-pool-size:3}")
    private int dataTaskCorePoolSize;
    
    @Value("${message.async.data-task.max-pool-size:10}")
    private int dataTaskMaxPoolSize;
    
    @Value("${message.async.data-task.queue-capacity:100}")
    private int dataTaskQueueCapacity;
    
    @Value("${message.async.data-task.keep-alive-seconds:600}")
    private int dataTaskKeepAliveSeconds;
    
    @Value("${message.async.notification-task.core-pool-size:2}")
    private int notificationTaskCorePoolSize;
    
    @Value("${message.async.notification-task.max-pool-size:8}")
    private int notificationTaskMaxPoolSize;
    
    @Value("${message.async.notification-task.queue-capacity:50}")
    private int notificationTaskQueueCapacity;
    
    @Value("${message.async.notification-task.keep-alive-seconds:300}")
    private int notificationTaskKeepAliveSeconds;
    
    /**
     * 消息任务执行器
     * 
     * 主要处理用户上线推送、实时消息发送、WebSocket通信等轻量级任务。
     * 这些任务执行时间短，但对响应时间要求高。
     * 
     * 适用场景：
     * - 用户上线推送
     * - 实时消息发送
     * - WebSocket消息推送
     * - 消息状态更新
     * - 缓存操作
     * 
     * @return 消息任务执行器
     */
    @Bean("messageTaskExecutor")
    public Executor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(messageTaskCorePoolSize);
        executor.setMaxPoolSize(messageTaskMaxPoolSize);
        executor.setQueueCapacity(messageTaskQueueCapacity);
        executor.setKeepAliveSeconds(messageTaskKeepAliveSeconds);
        
        // 线程命名
        executor.setThreadNamePrefix("MessageTask-");
        
        // 拒绝策略：调用者运行策略，确保任务不丢失
        executor.setRejectedExecutionHandler(new MessageTaskRejectedExecutionHandler("MessageTask"));
        
        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);
        
        // 等待所有任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("消息任务执行器已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                messageTaskCorePoolSize, messageTaskMaxPoolSize, messageTaskQueueCapacity);
        
        return executor;
    }
    
    /**
     * 数据任务执行器
     * 
     * 主要处理数据库查询、数据统计、批量处理等重量级任务。
     * 这些任务可能执行时间较长，但对系统稳定性要求高。
     * 
     * 适用场景：
     * - 大批量消息查询
     * - 历史数据统计
     * - 数据库批量操作
     * - 文件处理
     * - 数据同步
     * 
     * @return 数据任务执行器
     */
    @Bean("dataTaskExecutor")
    public Executor dataTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(dataTaskCorePoolSize);
        executor.setMaxPoolSize(dataTaskMaxPoolSize);
        executor.setQueueCapacity(dataTaskQueueCapacity);
        executor.setKeepAliveSeconds(dataTaskKeepAliveSeconds);
        
        // 线程命名
        executor.setThreadNamePrefix("DataTask-");
        
        // 拒绝策略：记录日志并丢弃任务
        executor.setRejectedExecutionHandler(new MessageTaskRejectedExecutionHandler("DataTask"));
        
        // 不允许核心线程超时，保持基本处理能力
        executor.setAllowCoreThreadTimeOut(false);
        
        // 等待所有任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        
        executor.initialize();
        
        log.info("数据任务执行器已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                dataTaskCorePoolSize, dataTaskMaxPoolSize, dataTaskQueueCapacity);
        
        return executor;
    }
    
    /**
     * 通知任务执行器
     * 
     * 主要处理外部通知任务，如邮件发送、短信通知、第三方API调用等。
     * 这些任务依赖外部服务，可能存在网络延迟或失败。
     * 
     * 适用场景：
     * - 邮件通知发送
     * - 短信通知发送
     * - 第三方系统通知
     * - Webhook调用
     * - 外部API集成
     * 
     * @return 通知任务执行器
     */
    @Bean("notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 基础配置
        executor.setCorePoolSize(notificationTaskCorePoolSize);
        executor.setMaxPoolSize(notificationTaskMaxPoolSize);
        executor.setQueueCapacity(notificationTaskQueueCapacity);
        executor.setKeepAliveSeconds(notificationTaskKeepAliveSeconds);
        
        // 线程命名
        executor.setThreadNamePrefix("NotificationTask-");
        
        // 拒绝策略：记录日志并重试
        executor.setRejectedExecutionHandler(new MessageTaskRejectedExecutionHandler("NotificationTask"));
        
        // 允许核心线程超时，节省资源
        executor.setAllowCoreThreadTimeOut(true);
        
        // 等待所有任务完成后关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);
        
        executor.initialize();
        
        log.info("通知任务执行器已初始化: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                notificationTaskCorePoolSize, notificationTaskMaxPoolSize, notificationTaskQueueCapacity);
        
        return executor;
    }
    
    /**
     * 自定义拒绝策略
     * 
     * 当线程池和队列都满时的处理策略，记录详细日志便于监控和调试。
     */
    private static class MessageTaskRejectedExecutionHandler implements RejectedExecutionHandler {
        
        private final String executorName;
        
        public MessageTaskRejectedExecutionHandler(String executorName) {
            this.executorName = executorName;
        }
        
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("异步任务被拒绝执行: executor={}, task={}, activeCount={}, poolSize={}, queueSize={}", 
                    executorName,
                    r.getClass().getSimpleName(),
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size());
            
            // 根据执行器类型采用不同策略
            if ("MessageTask".equals(executorName)) {
                // 消息任务：由调用者线程执行，确保不丢失
                try {
                    r.run();
                    log.debug("消息任务由调用者线程执行完成: task={}", r.getClass().getSimpleName());
                } catch (Exception e) {
                    log.error("消息任务执行失败: task={}, error={}", r.getClass().getSimpleName(), e.getMessage());
                }
            } else if ("DataTask".equals(executorName)) {
                // 数据任务：记录日志，可以接受丢失
                log.error("数据任务被丢弃: task={}", r.getClass().getSimpleName());
            } else if ("NotificationTask".equals(executorName)) {
                // 通知任务：记录日志，后续可以重试
                log.warn("通知任务被丢弃: task={}", r.getClass().getSimpleName());
                // 可以在这里添加重试逻辑或将任务存储到持久化队列
            } else {
                // 其他任务：默认记录日志
                log.warn("未知类型任务被拒绝: executor={}, task={}", executorName, r.getClass().getSimpleName());
            }
        }
    }
}
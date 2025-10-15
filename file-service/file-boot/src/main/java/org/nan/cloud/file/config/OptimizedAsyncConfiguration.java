package org.nan.cloud.file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 优化的异步配置
 * 替换现有AsyncConfiguration，提供更精细的线程池配置
 * 
 * 根据Backend专家的可靠性要求：
 * - 资源控制：<500MB内存使用
 * - 性能目标：<200ms响应时间
 * - 可靠性：99.9%可用性
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class OptimizedAsyncConfiguration {

    /**
     * 缩略图生成专用线程池
     * 针对CPU密集型任务优化，限制并发避免系统过载
     */
    @Bean("thumbnailExecutor")
    public ExecutorService thumbnailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // 缩略图生成是CPU密集型，核心线程数 = CPU核心数
        executor.setCorePoolSize(cpuCores);
        
        // 最大线程数限制，避免CPU争抢
        executor.setMaxPoolSize(cpuCores + 2);
        
        // 队列容量适中，避免内存过度占用
        executor.setQueueCapacity(50);
        
        // 线程空闲时间
        executor.setKeepAliveSeconds(300);
        
        executor.setThreadNamePrefix("Thumbnail-");
        
        // 任务拒绝策略：调用线程执行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("缩略图线程池已初始化 - 核心: {}, 最大: {}, 队列: {}", 
                cpuCores, cpuCores + 2, 50);
        
        return executor.getThreadPoolExecutor();
    }

    /**
     * 文件上传任务执行器
     * 优化的IO密集型线程池，替代原AsyncConfiguration中的fileUploadTaskExecutor
     */
    @Bean("fileUploadTaskExecutor")
    public Executor fileUploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        int cpuCores = Runtime.getRuntime().availableProcessors();
        
        // 文件上传是IO密集型，可以设置更多线程
        executor.setCorePoolSize(cpuCores * 2);
        executor.setMaxPoolSize(cpuCores * 4);
        
        // 上传队列容量，平衡内存使用和响应性
        executor.setQueueCapacity(200);
        
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("FileUpload-");
        
        // 文件上传使用CallerRuns策略，确保任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("文件上传异步执行器已初始化 - 核心: {}, 最大: {}, 队列: {}", 
                cpuCores * 2, cpuCores * 4, 200);
        
        return executor;
    }

    /**
     * 通用任务执行器
     * 整合原AsyncConfiguration中的commonTaskExecutor
     */
    @Bean("commonTaskExecutor")
    public Executor commonTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("Common-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("通用异步执行器已初始化");
        
        return executor;
    }

    /**
     * 轻量级任务执行器
     * 处理不重要的后台任务，避免影响核心功能
     */
    @Bean("lightWeightTaskExecutor")
    public Executor lightWeightTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("LightWeight-");
        
        // 轻量级任务可以直接丢弃
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        
        executor.initialize();
        
        log.info("轻量级任务线程池已初始化");
        
        return executor;
    }
}
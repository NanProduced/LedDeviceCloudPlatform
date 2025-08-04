package org.nan.cloud.file.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 
 * 配置文件上传服务的异步执行器，优化文件上传性能和用户体验。
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * 文件上传任务执行器
     * 
     * 专门用于处理文件上传的异步任务，包括：
     * - 文件存储处理
     * - 缩略图生成
     * - 进度更新和事件发布
     * 
     * @return 线程池任务执行器
     */
    @Bean("fileUploadTaskExecutor")
    public Executor fileUploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数：CPU核心数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数：核心线程数的2倍
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量：100个任务
        executor.setQueueCapacity(100);
        
        // 线程空闲时间：60秒
        executor.setKeepAliveSeconds(60);
        
        // 线程名前缀
        executor.setThreadNamePrefix("FileUpload-");
        
        // 拒绝策略：由调用者线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：30秒
        executor.setAwaitTerminationSeconds(30);
        
        // 初始化执行器
        executor.initialize();
        
        log.info("文件上传异步执行器已初始化 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    /**
     * 通用任务执行器
     * 
     * 用于处理其他轻量级异步任务
     * 
     * @return 通用任务执行器
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
}
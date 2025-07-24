package org.nan.cloud.terminal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 定时任务配置
 * 
 * 为终端服务配置独立的定时任务线程池，避免定时任务互相影响
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 创建专用的定时任务线程池
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(4, r -> {
            Thread thread = new Thread(r, "terminal-schedule-");
            thread.setDaemon(true);
            return thread;
        });
        
        taskRegistrar.setScheduler(executor);
    }
}
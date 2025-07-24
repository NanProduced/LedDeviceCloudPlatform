package org.nan.cloud.terminal.config.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 简化的MySQL数据库配置
 * 
 * 使用Spring Boot的自动配置，配置从application.yml读取：
 * - HikariCP连接池自动配置
 * - 数据库连接参数自动注入
 * - 性能参数在配置文件中设置
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class SimpleMySQLConfig {

    // 这个类主要用于标记MySQL配置的存在
    // 实际配置通过application.yml进行，Spring Boot会自动创建HikariCP数据源
    
    public SimpleMySQLConfig() {
        log.info("MySQL配置已加载，使用Spring Boot自动配置的HikariCP连接池");
    }
}
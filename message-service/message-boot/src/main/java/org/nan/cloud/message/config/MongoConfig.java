package org.nan.cloud.message.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB配置类
 * 
 * 启用MongoDB Repository扫描
 * 连接配置通过application.yml配置文件管理
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Configuration
@EnableMongoRepositories(basePackages = "org.nan.cloud.message.infrastructure.mongodb")
public class MongoConfig {


}
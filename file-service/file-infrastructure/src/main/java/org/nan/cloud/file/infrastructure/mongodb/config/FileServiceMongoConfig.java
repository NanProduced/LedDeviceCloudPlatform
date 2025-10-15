package org.nan.cloud.file.infrastructure.mongodb.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * File-Service MongoDB配置
 * 
 * 策略变更：
 * 1. 不全局禁用_class字段，避免影响其他MongoDB文档类型
 * 2. MaterialMetadata使用统一模型，无需特殊配置
 * 3. 依赖Spring Data MongoDB的默认行为
 * 
 * 说明：
 * - 由于file-service和core-service都使用相同的MaterialMetadata类
 * - MongoDB会为相同的类路径生成相同的_class字段值
 * - 因此不需要禁用_class字段，自然兼容
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class FileServiceMongoConfig {

    // 移除自定义配置，使用Spring Data MongoDB默认行为
    // 统一模型确保了跨服务兼容性
    
}
package org.nan.cloud.file.config;

import org.nan.cloud.file.application.config.FileStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件服务自动配置
 * 
 * 启用配置属性处理，确保IDE能够正确识别application.yml中的配置项
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties({
    FileStorageProperties.class
})
public class FileServiceAutoConfiguration {
    
    // 这个配置类主要用于启用配置属性处理
    // 确保IDE能够正确识别和提示application.yml中的file.*配置项
}

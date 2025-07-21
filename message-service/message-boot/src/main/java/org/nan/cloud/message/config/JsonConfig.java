package org.nan.cloud.message.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * JSON配置类
 * 
 * 确保ObjectMapper Bean被正确创建和配置
 * 
 * @author LedDeviceCloudPlatform Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class JsonConfig {
    
    /**
     * 创建和配置ObjectMapper Bean
     * 
     * 添加@Primary注解确保这个Bean是默认的ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("创建ObjectMapper Bean");
        
        ObjectMapper mapper = new ObjectMapper();
        
        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 设置属性命名策略为驼峰命名
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        
        // 忽略未知属性
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        log.info("✅ ObjectMapper Bean创建成功");
        return mapper;
    }
}
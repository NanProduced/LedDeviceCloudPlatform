package org.nan.cloud.auth.boot.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.nan.cloud.auth.infrastructure.security.UserPrincipal;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Collection;
import java.util.Map;

@Configuration
public class SessionConfig implements BeanClassLoaderAware {

    private ClassLoader loader;

    /**
     * 设置Redis Json序列化
     * @return
     */
    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new GenericJackson2JsonRedisSerializer(objectMapper());
    }

    /**
     * Customized {@link ObjectMapper} to add mix-in for class that doesn't have default
     * constructors
     * @return the {@link ObjectMapper} to use
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        mapper.addMixIn(UserPrincipal.class, UserPrincipalMixin.class);
        mapper.activateDefaultTyping(ptv(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    private PolymorphicTypeValidator ptv() {
        return BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("org.springframework.security.core")
                .allowIfSubType("org.springframework.security.web")
                .allowIfSubType("org.springframework.security.web.savedrequest")
                .allowIfSubType("org.nan.cloud.auth.infrastructure.security")
                .allowIfSubType("org.nan.cloud.auth.boot.login")
                .allowIfSubType("org.springframework.security.authentication")
                .allowIfSubType("java.lang")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build();
    }

    /**
     * @see org.springframework.beans.factory.BeanClassLoaderAware#setBeanClassLoader(ClassLoader)
     */
    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.loader = classLoader;
    }

    /**
     * 注册Session事件发布器
     * @return
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static abstract class UserPrincipalMixin { }

}

package org.nan.cloud.core.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;

/**
 * 缓存注解使用示例服务接口
 * 展示如何在业务服务中使用Spring Cache注解
 */
public interface CacheableService {
    
    /**
     * 查询用户信息 - 使用缓存
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 用户信息
     */
    @Cacheable(value = "user-info", key = "'org:' + #orgId + ':user:' + #userId")
    <T> T getUserInfo(Long userId, Long orgId, Class<T> clazz);
    
    /**
     * 查询终端组信息 - 使用缓存
     * @param tgid 终端组ID
     * @param orgId 组织ID
     * @return 终端组信息
     */
    @Cacheable(value = "terminal-group", key = "'org:' + #orgId + ':tg:' + #tgid")
    <T> T getTerminalGroup(Long tgid, Long orgId, Class<T> clazz);
    
    /**
     * 查询用户权限 - 使用缓存
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param url 请求URL
     * @param method 请求方法
     * @return 是否有权限
     */
    @Cacheable(value = "user-permissions", 
               key = "'org:' + #orgId + ':permission:' + #userId + ':' + #url + ':' + #method")
    Boolean getUserPermission(Long userId, Long orgId, String url, String method);
    
    /**
     * 更新用户信息 - 更新缓存
     * @param user 用户信息
     * @param userId 用户ID
     * @param orgId 组织ID
     * @return 更新后的用户信息
     */
    @CachePut(value = "user-info", key = "'org:' + #orgId + ':user:' + #userId")
    <T> T updateUserInfo(Object user, Long userId, Long orgId);
    
    /**
     * 删除用户 - 清理相关缓存
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Caching(evict = {
        @CacheEvict(value = "user-info", key = "'org:' + #orgId + ':user:' + #userId"),
        @CacheEvict(value = "user-permissions", key = "'org:' + #orgId + ':permission:' + #userId + ':*'")
    })
    void deleteUser(Long userId, Long orgId);
    
    /**
     * 清理组织所有缓存
     * @param orgId 组织ID
     */
    @Caching(evict = {
        @CacheEvict(value = "user-info", key = "'org:' + #orgId + ':*'"),
        @CacheEvict(value = "terminal-group", key = "'org:' + #orgId + ':*'"),
        @CacheEvict(value = "user-permissions", key = "'org:' + #orgId + ':*'")
    })
    void clearOrganizationCache(Long orgId);
}
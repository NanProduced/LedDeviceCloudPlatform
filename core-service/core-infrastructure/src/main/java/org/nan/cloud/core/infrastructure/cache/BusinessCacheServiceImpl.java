package org.nan.cloud.core.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.core.enums.CacheType;
import org.nan.cloud.core.service.BusinessCacheService;
import org.nan.cloud.core.service.CacheService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务缓存服务实现
 * 支持组织级别的缓存隔离
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessCacheServiceImpl implements BusinessCacheService {
    
    private final CacheService cacheService;
    
    // 权限相关缓存（按组织隔离）
    @Override
    public Boolean getUserPermission(Long userId, Long orgId, String url, String method) {
        String key = CacheType.USER_PERMISSIONS.buildOrgKey(orgId, userId.toString(), url, method);
        return cacheService.get(key, Boolean.class);
    }
    
    @Override
    public void cacheUserPermission(Long userId, Long orgId, String url, String method, Boolean hasPermission) {
        String key = CacheType.USER_PERMISSIONS.buildOrgKey(orgId, userId.toString(), url, method);
        cacheService.put(key, hasPermission, CacheType.USER_PERMISSIONS.getDefaultTtl());
        log.debug("缓存用户权限: orgId={}, userId={}, url={}, method={}, permission={}", 
            orgId, userId, url, method, hasPermission);
    }
    
    @Override
    public void evictUserPermissions(Long userId, Long orgId) {
        String pattern = CacheType.USER_PERMISSIONS.buildOrgKey(orgId, userId.toString()) + ":*";
        cacheService.evictByPattern(pattern);
        log.debug("清理用户权限缓存: orgId={}, userId={}", orgId, userId);
    }
    
    @Override
    public void evictAllUserPermissions(Long orgId) {
        String pattern = CacheType.USER_PERMISSIONS.buildOrgPattern(orgId);
        cacheService.evictByPattern(pattern);
        log.debug("清理组织内所有用户权限缓存: orgId={}", orgId);
    }
    
    // 用户组权限缓存
    @Override
    public <T> T getUserGroupPermissionStatus(Long ugid, Long orgId, Class<T> clazz) {
        String key = CacheType.PERMISSION_EXPRESSION.buildOrgKey(orgId, "usergroup", ugid.toString());
        return cacheService.get(key, clazz);
    }
    
    @Override
    public void cacheUserGroupPermissionStatus(Long ugid, Long orgId, Object status) {
        String key = CacheType.PERMISSION_EXPRESSION.buildOrgKey(orgId, "usergroup", ugid.toString());
        cacheService.put(key, status, CacheType.PERMISSION_EXPRESSION.getDefaultTtl());
        log.debug("缓存用户组权限状态: orgId={}, ugid={}", orgId, ugid);
    }
    
    @Override
    public void evictUserGroupPermissionStatus(Long ugid, Long orgId) {
        String key = CacheType.PERMISSION_EXPRESSION.buildOrgKey(orgId, "usergroup", ugid.toString());
        cacheService.evict(key);
        log.debug("清理用户组权限状态缓存: orgId={}, ugid={}", orgId, ugid);
    }
    
    @Override
    public void evictAllUserGroupPermissions(Long orgId) {
        String pattern = CacheType.PERMISSION_EXPRESSION.buildOrgPattern(orgId);
        cacheService.evictByPattern(pattern);
        log.debug("清理组织内所有用户组权限缓存: orgId={}", orgId);
    }
    
    // 终端组缓存（按组织隔离）
    @Override
    public <T> T getTerminalGroupTree(Long orgId, Class<T> clazz) {
        String key = CacheType.TERMINAL_GROUP_TREE.buildOrgKey(orgId, "tree");
        return cacheService.get(key, clazz);
    }
    
    @Override
    public void cacheTerminalGroupTree(Long orgId, Object tree) {
        String key = CacheType.TERMINAL_GROUP_TREE.buildOrgKey(orgId, "tree");
        cacheService.put(key, tree, CacheType.TERMINAL_GROUP_TREE.getDefaultTtl());
        log.debug("缓存终端组树: orgId={}", orgId);
    }
    
    @Override
    public void evictTerminalGroupTree(Long orgId) {
        String key = CacheType.TERMINAL_GROUP_TREE.buildOrgKey(orgId, "tree");
        cacheService.evict(key);
        log.debug("清理终端组树缓存: orgId={}", orgId);
    }
    
    @Override
    public <T> T getTerminalGroup(Long tgid, Long orgId, Class<T> clazz) {
        String key = CacheType.TERMINAL_GROUP_INFO.buildOrgKey(orgId, tgid.toString());
        return cacheService.get(key, clazz);
    }
    
    @Override
    public void cacheTerminalGroup(Object terminalGroup, Long tgid, Long orgId) {
        String key = CacheType.TERMINAL_GROUP_INFO.buildOrgKey(orgId, tgid.toString());
        cacheService.put(key, terminalGroup, CacheType.TERMINAL_GROUP_INFO.getDefaultTtl());
        log.debug("缓存终端组: orgId={}, tgid={}", orgId, tgid);
    }
    
    @Override
    public void evictTerminalGroup(Long tgid, Long orgId) {
        String key = CacheType.TERMINAL_GROUP_INFO.buildOrgKey(orgId, tgid.toString());
        cacheService.evict(key);
        log.debug("清理终端组缓存: orgId={}, tgid={}", orgId, tgid);
    }
    
    @Override
    public void evictAllTerminalGroups(Long orgId) {
        String pattern = CacheType.TERMINAL_GROUP_INFO.buildOrgPattern(orgId);
        cacheService.evictByPattern(pattern);
        log.debug("清理组织内所有终端组缓存: orgId={}", orgId);
    }
    
    // 用户相关缓存（按组织隔离）
    @Override
    public <T> T getUser(Long userId, Long orgId, Class<T> clazz) {
        String key = CacheType.USER_INFO.buildOrgKey(orgId, userId.toString());
        return cacheService.get(key, clazz);
    }
    
    @Override
    public void cacheUser(Object user, Long userId, Long orgId) {
        String key = CacheType.USER_INFO.buildOrgKey(orgId, userId.toString());
        cacheService.put(key, user, CacheType.USER_INFO.getDefaultTtl());
        log.debug("缓存用户信息: orgId={}, userId={}", orgId, userId);
    }
    
    @Override
    public void evictUser(Long userId, Long orgId) {
        String key = CacheType.USER_INFO.buildOrgKey(orgId, userId.toString());
        cacheService.evict(key);
        log.debug("清理用户信息缓存: orgId={}, userId={}", orgId, userId);
    }
    
    @Override
    public void evictAllUsers(Long orgId) {
        String pattern = CacheType.USER_INFO.buildOrgPattern(orgId);
        cacheService.evictByPattern(pattern);
        log.debug("清理组织内所有用户缓存: orgId={}", orgId);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getUserGroups(Long orgId, Class<T> clazz) {
        String key = CacheType.USER_GROUPS.buildOrgKey(orgId, "list");
        return cacheService.get(key, List.class);
    }
    
    @Override
    public void cacheUserGroups(Long orgId, List<?> userGroups) {
        String key = CacheType.USER_GROUPS.buildOrgKey(orgId, "list");
        cacheService.put(key, userGroups, CacheType.USER_GROUPS.getDefaultTtl());
        log.debug("缓存用户组列表: orgId={}, count={}", orgId, userGroups.size());
    }
    
    @Override
    public void evictUserGroups(Long orgId) {
        String key = CacheType.USER_GROUPS.buildOrgKey(orgId, "list");
        cacheService.evict(key);
        log.debug("清理用户组列表缓存: orgId={}", orgId);
    }
    
    // 组织级别缓存管理
    @Override
    public void evictOrganizationCache(Long orgId) {
        log.info("开始清理组织缓存: orgId={}", orgId);
        
        // 清理各类业务缓存
        evictAllUserPermissions(orgId);
        evictAllUserGroupPermissions(orgId);
        evictAllTerminalGroups(orgId);
        evictAllUsers(orgId);
        evictUserGroups(orgId);
        evictTerminalGroupTree(orgId);
        
        log.info("组织缓存清理完成: orgId={}", orgId);
    }
    
    @Override
    public void evictMultiOrganizationCache(List<Long> orgIds) {
        log.info("开始批量清理组织缓存: orgIds={}", orgIds);
        orgIds.forEach(this::evictOrganizationCache);
        log.info("批量组织缓存清理完成: count={}", orgIds.size());
    }
    
    // 全局缓存（不按组织隔离的系统级缓存）
    @Override
    public <T> T getSystemConfig(String configKey, Class<T> clazz) {
        String key = CacheType.SYSTEM_CONFIG.buildKey(configKey);
        return cacheService.get(key, clazz);
    }
    
    @Override
    public void cacheSystemConfig(String configKey, Object value) {
        String key = CacheType.SYSTEM_CONFIG.buildKey(configKey);
        cacheService.put(key, value, CacheType.SYSTEM_CONFIG.getDefaultTtl());
        log.debug("缓存系统配置: key={}", configKey);
    }
    
    @Override
    public void evictSystemConfig(String configKey) {
        String key = CacheType.SYSTEM_CONFIG.buildKey(configKey);
        cacheService.evict(key);
        log.debug("清理系统配置缓存: key={}", configKey);
    }
}
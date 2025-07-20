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


    // 用户组可见终端组列表缓存
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getUserAccessibleTerminalGroupIds(Long ugid, Long orgId, Class<T> clazz) {
        String key = CacheType.TERMINAL_GROUP_PERMISSIONS.buildOrgKey(orgId, "accessible", ugid.toString());
        return cacheService.get(key, List.class);
    }

    @Override
    public void cacheUserAccessibleTerminalGroupIds(Long ugid, Long orgId, List<?> terminalGroupIds) {
        String key = CacheType.TERMINAL_GROUP_PERMISSIONS.buildOrgKey(orgId, "accessible", ugid.toString());
        cacheService.put(key, terminalGroupIds, CacheType.TERMINAL_GROUP_PERMISSIONS.getDefaultTtl());
        log.debug("缓存用户组可见终端组列表: orgId={}, ugid={}, count={}", orgId, ugid, terminalGroupIds.size());
    }

    @Override
    public void evictUserAccessibleTerminalGroupIds(Long ugid, Long orgId) {
        String key = CacheType.TERMINAL_GROUP_PERMISSIONS.buildOrgKey(orgId, "accessible", ugid.toString());
        cacheService.evict(key);
        log.debug("清理用户组可见终端组列表缓存: orgId={}, ugid={}", orgId, ugid);
    }

    @Override
    public void evictAllUserAccessibleTerminalGroups(Long orgId) {
        String pattern = CacheType.TERMINAL_GROUP_PERMISSIONS.buildOrgKey(orgId, "accessible") + ":*";
        cacheService.evictByPattern(pattern);
        log.debug("清理组织内所有用户组可见终端组缓存: orgId={}", orgId);
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
        evictAllUserGroupPermissions(orgId);
        evictAllUserAccessibleTerminalGroups(orgId);
        evictAllTerminalGroups(orgId);
        evictUserGroups(orgId);
        
        log.info("组织缓存清理完成: orgId={}", orgId);
    }
    
    @Override
    public void evictMultiOrganizationCache(List<Long> orgIds) {
        log.info("开始批量清理组织缓存: orgIds={}", orgIds);
        orgIds.forEach(this::evictOrganizationCache);
        log.info("批量组织缓存清理完成: count={}", orgIds.size());
    }

}
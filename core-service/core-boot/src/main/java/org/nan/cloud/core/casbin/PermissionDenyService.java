package org.nan.cloud.core.casbin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.main.Enforcer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 权限拒绝管理服务
 * 用于管理特定用户或角色的权限禁用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionDenyService {
    
    @Qualifier("rbacEnforcer")
    private final Enforcer rbacEnforcer;
    
    /**
     * 为特定用户添加拒绝权限
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     * @param reason 拒绝原因
     */
    @Transactional
    public void addUserDenyPermission(Long userId, Long orgId, String url, String method, String reason) {
        boolean added = rbacEnforcer.addPolicy(
            userId.toString(),
            orgId.toString(), 
            url,
            method,
            "deny"
        );
        
        if (added) {
            log.info("已为用户添加拒绝权限: userId={}, orgId={}, url={}, method={}, reason={}", 
                userId, orgId, url, method, reason);
        } else {
            log.warn("拒绝权限已存在: userId={}, orgId={}, url={}, method={}", 
                userId, orgId, url, method);
        }
    }
    
    /**
     * 为特定角色添加拒绝权限
     * @param roleId 角色ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     * @param reason 拒绝原因
     */
    @Transactional
    public void addRoleDenyPermission(Long roleId, Long orgId, String url, String method, String reason) {
        boolean added = rbacEnforcer.addPolicy(
            roleId.toString(),
            orgId.toString(),
            url, 
            method,
            "deny"
        );
        
        if (added) {
            log.info("已为角色添加拒绝权限: roleId={}, orgId={}, url={}, method={}, reason={}", 
                roleId, orgId, url, method, reason);
        } else {
            log.warn("拒绝权限已存在: roleId={}, orgId={}, url={}, method={}", 
                roleId, orgId, url, method);
        }
    }
    
    /**
     * 移除用户的拒绝权限
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     */
    @Transactional
    public void removeUserDenyPermission(Long userId, Long orgId, String url, String method) {
        boolean removed = rbacEnforcer.removePolicy(
            userId.toString(),
            orgId.toString(),
            url,
            method, 
            "deny"
        );
        
        if (removed) {
            log.info("已移除用户拒绝权限: userId={}, orgId={}, url={}, method={}", 
                userId, orgId, url, method);
        } else {
            log.warn("拒绝权限不存在: userId={}, orgId={}, url={}, method={}", 
                userId, orgId, url, method);
        }
    }
    
    /**
     * 移除角色的拒绝权限
     * @param roleId 角色ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     */
    @Transactional
    public void removeRoleDenyPermission(Long roleId, Long orgId, String url, String method) {
        boolean removed = rbacEnforcer.removePolicy(
            roleId.toString(),
            orgId.toString(),
            url,
            method,
            "deny"
        );
        
        if (removed) {
            log.info("已移除角色拒绝权限: roleId={}, orgId={}, url={}, method={}", 
                roleId, orgId, url, method);
        } else {
            log.warn("拒绝权限不存在: roleId={}, orgId={}, url={}, method={}", 
                roleId, orgId, url, method);
        }
    }
    
    /**
     * 批量移除用户在指定组织的所有拒绝权限
     * @param userId 用户ID
     * @param orgId 组织ID
     */
    @Transactional
    public void removeAllUserDenyPermissions(Long userId, Long orgId) {
        boolean removed = rbacEnforcer.removeFilteredPolicy(
            0, 
            userId.toString(), 
            orgId.toString(), 
            "", 
            "", 
            "deny"
        );
        
        if (removed) {
            log.info("已移除用户所有拒绝权限: userId={}, orgId={}", userId, orgId);
        }
    }
    
    /**
     * 检查用户是否被拒绝访问特定接口
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     * @return true 如果被拒绝
     */
    public boolean isUserDenied(Long userId, Long orgId, String url, String method) {
        // 检查直接的用户拒绝策略
        return rbacEnforcer.hasPolicy(
            userId.toString(),
            orgId.toString(),
            url,
            method,
            "deny"
        );
    }
    
    /**
     * 临时禁用用户访问特定接口（带过期时间）
     * 注意：这需要配合定时任务实现自动解除
     * @param userId 用户ID
     * @param orgId 组织ID
     * @param url 接口URL
     * @param method HTTP方法
     * @param durationHours 禁用时长（小时）
     * @param reason 禁用原因
     */
    @Transactional
    public void temporaryDenyUser(Long userId, Long orgId, String url, String method, 
                                int durationHours, String reason) {
        // 添加拒绝权限
        addUserDenyPermission(userId, orgId, url, method, 
            String.format("临时禁用%d小时: %s", durationHours, reason));
        
        // 这里可以结合Redis或其他方式实现定时解除
        // 例如：redisTemplate.opsForValue().set("deny:" + userId + ":" + orgId + ":" + url + ":" + method, 
        //                                      reason, Duration.ofHours(durationHours));
        
        log.info("用户已被临时禁用: userId={}, orgId={}, url={}, method={}, hours={}, reason={}", 
            userId, orgId, url, method, durationHours, reason);
    }
}
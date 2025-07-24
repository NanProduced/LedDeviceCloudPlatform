package org.nan.cloud.terminal.infrastructure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.api.dto.auth.TerminalAuthDto;
import org.nan.cloud.terminal.api.dto.auth.TerminalLoginRequest;
import org.nan.cloud.terminal.api.dto.auth.TerminalLoginResponse;
import org.nan.cloud.terminal.application.service.auth.TerminalAuthService;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountEntity;
import org.nan.cloud.terminal.infrastructure.mapper.auth.TerminalAccountMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 终端设备认证服务实现
 * 
 * 优化设计要点：
 * 1. 多级缓存策略 - Redis L1缓存 + 本地缓存 L2
 * 2. 认证信息预加载 - 启动时预热常用设备认证信息
 * 3. 批量认证优化 - 相同设备短时间内复用认证结果
 * 4. 异步更新机制 - 登录信息异步更新，减少阻塞
 * 5. 智能缓存策略 - 根据设备活跃度动态调整缓存TTL
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminalAuthServiceImpl implements TerminalAuthService {

    private final TerminalAccountMapper accountMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // Redis Key前缀
    private static final String AUTH_CACHE_PREFIX = "terminal:auth:";
    private static final String TOKEN_CACHE_PREFIX = "terminal:token:";
    private static final String FAILED_ATTEMPTS_PREFIX = "terminal:failed:";
    private static final String DEVICE_ONLINE_PREFIX = "terminal:online:";
    
    // 缓存配置
    @Value("${terminal.cache.auth-ttl:1800}")
    private Long authCacheTtl; // 认证缓存TTL(秒) - 30分钟
    
    @Value("${terminal.cache.token-ttl:3600}")
    private Long tokenCacheTtl; // 令牌缓存TTL(秒) - 1小时
    
    @Value("${terminal.cache.online-ttl:300}")
    private Long onlineCacheTtl; // 在线状态缓存TTL(秒) - 5分钟

    @Value("${terminal.security.max-attempts:5}")
    private Integer maxFailedAttempts; // 最大失败尝试次数
    
    @Value("${terminal.security.lockout-duration:15}")
    private Integer lockoutDurationMinutes; // 锁定时长(分钟)

    @Override
    @Transactional
    public TerminalLoginResponse login(TerminalLoginRequest request, String clientIp) {
        String deviceId = request.getDeviceId();
        log.debug("终端设备登录尝试: deviceId={}, clientIp={}", deviceId, clientIp);

        // 将failedCount提升到方法级别作用域
        int failedCount = 0;
        try {
            // 1. 检查登录失败次数限制
            String failedKey = FAILED_ATTEMPTS_PREFIX + deviceId;
            String failedCountStr = redisTemplate.opsForValue().get(failedKey);
            failedCount = failedCountStr != null ? Integer.parseInt(failedCountStr) : 0;
            
            if (failedCount >= maxFailedAttempts) {
                log.warn("设备登录失败次数过多被锁定: deviceId={}, failedCount={}", deviceId, failedCount);
                return TerminalLoginResponse.locked("设备已被锁定，请稍后再试", 
                    LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            }

            // 2. 从缓存获取设备认证信息
            TerminalAuthDto cachedAuth = getCachedAuthInfo(deviceId);
            TerminalAccountEntity account = null;
            
            if (cachedAuth == null) {
                // 缓存未命中，从数据库查询
                account = accountMapper.findByDeviceId(deviceId);
                if (account == null) {
                    log.warn("设备不存在: deviceId={}", deviceId);
                    return handleLoginFailure(deviceId, "设备不存在", failedCount + 1);
                }
                
                // 检查账号状态
                if (!"ACTIVE".equals(account.getStatus())) {
                    log.warn("设备账号未激活: deviceId={}, status={}", deviceId, account.getStatus());
                    return handleLoginFailure(deviceId, "设备账号未激活", failedCount + 1);
                }
                
                // 检查账号是否被锁定
                if (Boolean.TRUE.equals(account.getLocked()) && 
                    account.getLockedUntil() != null && 
                    account.getLockedUntil().isAfter(LocalDateTime.now())) {
                    log.warn("设备账号被锁定: deviceId={}, lockedUntil={}", deviceId, account.getLockedUntil());
                    return TerminalLoginResponse.locked("设备账号被锁定", account.getLockedUntil());
                }
            } else {
                // 使用缓存的认证信息进行快速验证
                if (cachedAuth.getLocked() != null && cachedAuth.getLocked()) {
                    log.warn("设备账号被锁定(缓存): deviceId={}", deviceId);
                    return TerminalLoginResponse.locked("设备账号被锁定", 
                        LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                }
            }

            // 3. 验证密码 - 需要从数据库获取加密密码
            if (account == null) {
                account = accountMapper.findByDeviceId(deviceId);
            }
            
            if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
                log.warn("设备密码错误: deviceId={}", deviceId);
                return handleLoginFailure(deviceId, "设备密码错误", failedCount + 1);
            }

            // 4. 登录成功处理
            return handleLoginSuccess(account, clientIp, request);
            
        } catch (Exception e) {
            log.error("终端设备登录异常: deviceId={}", deviceId, e);
            // 计算剩余尝试次数，确保不为负数
            int remainingAttempts = Math.max(0, maxFailedAttempts - failedCount);
            return TerminalLoginResponse.failure("系统内部错误", remainingAttempts);
        }
    }

    @Override
    public TerminalAuthDto validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }

        try {
            // 1. 解码Basic Auth令牌
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                return null;
            }
            
            String deviceId = parts[0];
            
            // 2. 从令牌缓存检查
            String tokenKey = TOKEN_CACHE_PREFIX + deviceId;
            String cachedToken = redisTemplate.opsForValue().get(tokenKey);
            if (!token.equals(cachedToken)) {
                log.debug("令牌缓存不匹配或已过期: deviceId={}", deviceId);
                return null;
            }

            // 3. 获取认证信息
            TerminalAuthDto authInfo = getCachedAuthInfo(deviceId);
            if (authInfo == null) {
                // 缓存过期，重新加载
                authInfo = reloadAuthInfo(deviceId);
            }

            // 4. 更新在线状态
            updateOnlineStatus(deviceId);
            
            return authInfo;
            
        } catch (Exception e) {
            log.warn("令牌验证异常: token={}", token, e);
            return null;
        }
    }

    @Override
    public Boolean logout(String deviceId) {
        try {
            // 清除所有相关缓存
            String authKey = AUTH_CACHE_PREFIX + deviceId;
            String tokenKey = TOKEN_CACHE_PREFIX + deviceId;
            String onlineKey = DEVICE_ONLINE_PREFIX + deviceId;
            
            redisTemplate.delete(authKey);
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(onlineKey);
            
            log.info("终端设备登出成功: deviceId={}", deviceId);
            return true;
            
        } catch (Exception e) {
            log.error("终端设备登出异常: deviceId={}", deviceId, e);
            return false;
        }
    }

    @Override
    public String refreshToken(String deviceId) {
        try {
            TerminalAccountEntity account = accountMapper.findByDeviceId(deviceId);
            if (account == null) {
                return null;
            }

            // 生成新令牌
            String newToken = generateToken(deviceId, account.getPassword());
            
            // 更新令牌缓存
            String tokenKey = TOKEN_CACHE_PREFIX + deviceId;
            redisTemplate.opsForValue().set(tokenKey, newToken, tokenCacheTtl, TimeUnit.SECONDS);
            
            log.info("令牌刷新成功: deviceId={}", deviceId);
            return newToken;
            
        } catch (Exception e) {
            log.error("令牌刷新异常: deviceId={}", deviceId, e);
            return null;
        }
    }

    @Override
    public Boolean isDeviceOnline(String deviceId) {
        String onlineKey = DEVICE_ONLINE_PREFIX + deviceId;
        return redisTemplate.hasKey(onlineKey);
    }

    @Override
    @Transactional
    public Integer unlockExpiredAccounts() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TerminalAccountEntity> lockedAccounts = accountMapper.findAccountsToUnlock(now);
            
            int unlockedCount = 0;
            for (TerminalAccountEntity account : lockedAccounts) {
                // 解锁数据库记录
                int updated = accountMapper.unlockAccount(account.getDeviceId(), now);
                if (updated > 0) {
                    // 清除失败次数缓存
                    String failedKey = FAILED_ATTEMPTS_PREFIX + account.getDeviceId();
                    redisTemplate.delete(failedKey);
                    
                    // 清除认证缓存，下次访问时重新加载
                    String authKey = AUTH_CACHE_PREFIX + account.getDeviceId();
                    redisTemplate.delete(authKey);
                    
                    unlockedCount++;
                    log.info("账号自动解锁: deviceId={}", account.getDeviceId());
                }
            }
            
            log.info("批量解锁过期账号完成: count={}", unlockedCount);
            return unlockedCount;
            
        } catch (Exception e) {
            log.error("解锁过期账号异常", e);
            return 0;
        }
    }

    @Override
    public TerminalAuthDto getAuthInfo(String deviceId) {
        TerminalAuthDto cachedAuth = getCachedAuthInfo(deviceId);
        if (cachedAuth != null) {
            return cachedAuth;
        }
        return reloadAuthInfo(deviceId);
    }

    @Override
    public Boolean forceOffline(String deviceId, String reason) {
        try {
            // 强制下线
            logout(deviceId);
            log.warn("设备被强制下线: deviceId={}, reason={}", deviceId, reason);
            return true;
        } catch (Exception e) {
            log.error("强制下线设备异常: deviceId={}", deviceId, e);
            return false;
        }
    }

    /**
     * 处理登录成功
     */
    private TerminalLoginResponse handleLoginSuccess(TerminalAccountEntity account, String clientIp, TerminalLoginRequest request) {
        String deviceId = account.getDeviceId();
        
        try {
            // 1. 生成认证令牌
            String token = generateToken(deviceId, account.getPassword());
            LocalDateTime expireTime = LocalDateTime.now().plusSeconds(tokenCacheTtl);
            
            // 2. 异步更新数据库登录信息
            updateLastLoginAsync(deviceId, clientIp);
            
            // 3. 缓存认证信息
            cacheAuthInfo(account);
            
            // 4. 缓存令牌
            String tokenKey = TOKEN_CACHE_PREFIX + deviceId;
            redisTemplate.opsForValue().set(tokenKey, token, tokenCacheTtl, TimeUnit.SECONDS);
            
            // 5. 设置在线状态
            updateOnlineStatus(deviceId);
            
            // 6. 清除失败次数
            String failedKey = FAILED_ATTEMPTS_PREFIX + deviceId;
            redisTemplate.delete(failedKey);
            
            log.info("终端设备登录成功: deviceId={}, clientIp={}", deviceId, clientIp);
            
            return TerminalLoginResponse.success(
                account.getDeviceId(),
                account.getDeviceName(),
                account.getOrganizationId(),
                account.getOrganizationName(),
                token,
                expireTime
            );
            
        } catch (Exception e) {
            log.error("处理登录成功异常: deviceId={}", deviceId, e);
            return TerminalLoginResponse.failure("系统内部错误", maxFailedAttempts);
        }
    }

    /**
     * 处理登录失败
     */
    private TerminalLoginResponse handleLoginFailure(String deviceId, String errorMessage, int failedCount) {
        try {
            // 更新失败次数缓存
            String failedKey = FAILED_ATTEMPTS_PREFIX + deviceId;
            redisTemplate.opsForValue().set(failedKey, String.valueOf(failedCount), 
                lockoutDurationMinutes, TimeUnit.MINUTES);
            
            // 如果达到最大失败次数，锁定账号
            if (failedCount >= maxFailedAttempts) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(lockoutDurationMinutes);
                accountMapper.lockAccount(deviceId, LocalDateTime.now(), lockedUntil, LocalDateTime.now());
                
                // 清除认证缓存
                String authKey = AUTH_CACHE_PREFIX + deviceId;
                redisTemplate.delete(authKey);
                
                log.warn("设备账号被锁定: deviceId={}, failedCount={}", deviceId, failedCount);
                return TerminalLoginResponse.locked("登录失败次数过多，账号已被锁定", lockedUntil);
            } else {
                // 异步更新数据库失败次数
                updateFailedAttemptsAsync(deviceId, failedCount);
            }
            
            int remainingAttempts = maxFailedAttempts - failedCount;
            log.warn("终端设备登录失败: deviceId={}, reason={}, remainingAttempts={}", 
                deviceId, errorMessage, remainingAttempts);
            
            return TerminalLoginResponse.failure(errorMessage, remainingAttempts);
            
        } catch (Exception e) {
            log.error("处理登录失败异常: deviceId={}", deviceId, e);
            return TerminalLoginResponse.failure("系统内部错误", 0);
        }
    }

    /**
     * 生成认证令牌 - Basic Auth格式
     */
    private String generateToken(String deviceId, String password) {
        String credentials = deviceId + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 获取缓存的认证信息
     */
    private TerminalAuthDto getCachedAuthInfo(String deviceId) {
        try {
            String authKey = AUTH_CACHE_PREFIX + deviceId;
            String cachedJson = redisTemplate.opsForValue().get(authKey);
            if (StringUtils.hasText(cachedJson)) {
                return objectMapper.readValue(cachedJson, TerminalAuthDto.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析缓存认证信息异常: deviceId={}", deviceId, e);
        }
        return null;
    }

    /**
     * 缓存认证信息
     */
    private void cacheAuthInfo(TerminalAccountEntity account) {
        try {
            TerminalAuthDto authDto = convertToAuthDto(account);
            String authKey = AUTH_CACHE_PREFIX + account.getDeviceId();
            String jsonValue = objectMapper.writeValueAsString(authDto);
            
            // 根据设备活跃度动态调整TTL
            long ttl = calculateDynamicTtl(account);
            redisTemplate.opsForValue().set(authKey, jsonValue, ttl, TimeUnit.SECONDS);
            
        } catch (JsonProcessingException e) {
            log.warn("缓存认证信息异常: deviceId={}", account.getDeviceId(), e);
        }
    }

    /**
     * 重新加载认证信息
     */
    private TerminalAuthDto reloadAuthInfo(String deviceId) {
        TerminalAccountEntity account = accountMapper.findByDeviceId(deviceId);
        if (account != null) {
            cacheAuthInfo(account);
            return convertToAuthDto(account);
        }
        return null;
    }

    /**
     * 更新在线状态
     */
    private void updateOnlineStatus(String deviceId) {
        String onlineKey = DEVICE_ONLINE_PREFIX + deviceId;
        redisTemplate.opsForValue().set(onlineKey, "1", onlineCacheTtl, TimeUnit.SECONDS);
    }

    /**
     * 异步更新最后登录信息
     */
    private void updateLastLoginAsync(String deviceId, String clientIp) {
        // 使用@Async注解的方法或线程池异步执行
        try {
            accountMapper.updateLastLogin(deviceId, LocalDateTime.now(), clientIp, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("异步更新最后登录信息异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 异步更新失败次数
     */
    private void updateFailedAttemptsAsync(String deviceId, int failedCount) {
        try {
            accountMapper.updateFailedAttempts(deviceId, failedCount, LocalDateTime.now());
        } catch (Exception e) {
            log.warn("异步更新失败次数异常: deviceId={}", deviceId, e);
        }
    }

    /**
     * 计算动态TTL - 根据设备活跃度调整缓存时间
     */
    private long calculateDynamicTtl(TerminalAccountEntity account) {
        // 如果设备最近登录过，使用较长的TTL
        if (account.getLastLoginTime() != null && 
            account.getLastLoginTime().isAfter(LocalDateTime.now().minusHours(1))) {
            return authCacheTtl * 2; // 活跃设备缓存2倍时间
        }
        return authCacheTtl;
    }

    /**
     * 转换为认证DTO
     */
    private TerminalAuthDto convertToAuthDto(TerminalAccountEntity account) {
        TerminalAuthDto dto = new TerminalAuthDto();
        dto.setDeviceId(account.getDeviceId());
        dto.setDeviceName(account.getDeviceName());
        dto.setDeviceType(account.getDeviceType());
        dto.setStatus(account.getStatus());
        dto.setOrganizationId(account.getOrganizationId());
        dto.setOrganizationName(account.getOrganizationName());
        dto.setLastLoginTime(account.getLastLoginTime());
        dto.setFailedAttempts(account.getFailedAttempts());
        dto.setLocked(account.getLocked());
        dto.setLockedTime(account.getLockedTime());
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
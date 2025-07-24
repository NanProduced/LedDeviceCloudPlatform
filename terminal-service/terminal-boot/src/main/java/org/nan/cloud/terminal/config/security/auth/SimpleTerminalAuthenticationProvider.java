package org.nan.cloud.terminal.config.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * 简化的终端认证提供者
 * 
 * 临时实现，用于编译通过。后续会完善为完整的数据库认证。
 * 目前使用硬编码的测试账号进行认证。
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleTerminalAuthenticationProvider implements AuthenticationProvider {

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    // 临时测试账号配置
    private static final String TEST_DEVICE_ID = "TEST_DEVICE_001";
    private static final String TEST_PASSWORD = "test123456";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String deviceId = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (!StringUtils.hasText(deviceId) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("设备ID和密码不能为空");
        }

        // 检查是否被锁定
        String lockKey = "terminal:locked:" + deviceId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new BadCredentialsException("设备已被锁定，请稍后重试");
        }

        // 简化认证逻辑 - 暂时使用测试账号
        if (TEST_DEVICE_ID.equals(deviceId) && TEST_PASSWORD.equals(password)) {
            // 认证成功，缓存认证结果
            String authKey = "terminal:auth:success:" + deviceId;
            redisTemplate.opsForValue().set(authKey, "true", 30, TimeUnit.MINUTES);
            
            // 创建用户主体
            TerminalUserPrincipal principal = new TerminalUserPrincipal();
            principal.setDeviceId(deviceId);
            principal.setDeviceName("测试设备");
            principal.setOrganizationId("test_org");
            
            log.info("设备认证成功: deviceId={}", deviceId);
            return new UsernamePasswordAuthenticationToken(principal, password, new ArrayList<>());
        }

        // 认证失败，记录失败次数
        String failedKey = "terminal:auth:failed:" + deviceId;
        String failedCountStr = redisTemplate.opsForValue().get(failedKey);
        int failedCount = failedCountStr != null ? Integer.parseInt(failedCountStr) : 0;
        failedCount++;

        redisTemplate.opsForValue().set(failedKey, String.valueOf(failedCount), 15, TimeUnit.MINUTES);

        // 超过5次失败，锁定账号
        if (failedCount >= 5) {
            redisTemplate.opsForValue().set(lockKey, "true", 15, TimeUnit.MINUTES);
            log.warn("设备认证失败次数过多，已锁定: deviceId={}, failedCount={}", deviceId, failedCount);
            throw new BadCredentialsException("认证失败次数过多，设备已被锁定15分钟");
        }

        log.warn("设备认证失败: deviceId={}, failedCount={}", deviceId, failedCount);
        throw new BadCredentialsException("设备认证失败，剩余尝试次数: " + (5 - failedCount));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
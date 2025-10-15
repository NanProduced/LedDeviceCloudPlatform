package org.nan.cloud.terminal.config.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.application.domain.TerminalAccount;
import org.nan.cloud.terminal.application.domain.TerminalInfo;
import org.nan.cloud.terminal.application.repository.TerminalRepository;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountDO;
import org.nan.cloud.terminal.infrastructure.mapper.auth.TerminalAccountMapper;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.nan.cloud.terminal.infrastructure.config.RedisConfig.RedisKeys.AUTH_CACHE_PREFIX;

/**
 * 终端设备认证提供者
 * 
 * 实现Basic Auth认证流程：
 * 1. 从Basic Auth中解析用户名和密码
 * 2. 通过TerminalAccountMapper查询账号信息并验证密码
 * 3. 通过TerminalDeviceRepository查询终端详细信息
 * 4. 封装为TerminalPrincipal返回认证结果
 * 5. 更新最后登录信息和Redis缓存
 * 
 * @author terminal-service
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalAuthenticationProvider implements AuthenticationProvider {

    private final TerminalRepository terminalRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String accountName = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (!StringUtils.hasText(accountName) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("用户名和密码不能为空");
        }


        try {
            // 1. 先检查Redis缓存
            TerminalPrincipal cachedPrincipal = checkAuthenticationCache(accountName, password);
            if (cachedPrincipal != null) {
                log.debug("终端认证缓存命中: accountName={}, tid={}", accountName, cachedPrincipal.getTid());
                return new UsernamePasswordAuthenticationToken(cachedPrincipal, password, cachedPrincipal.getAuthorities());
            }

            // 2. 缓存未命中，查询数据库进行完整认证
            TerminalAccount account = terminalRepository.getAccountByName(accountName);
            if (account == null) {
                throw new BadCredentialsException("账号不存在");
            }

            // 3. 验证密码
            if (!passwordEncoder.matches(password, account.getPassword())) {
                throw new BadCredentialsException("密码错误");
            }

            // 4. 检查账号状态
            if (account.getStatus() != 0) {
                throw new DisabledException("账号已被禁用");
            }

            // 5. 查询终端详细信息
            TerminalInfo terminalInfo = terminalRepository.getInfoByTid(account.getTid());
            if (terminalInfo == null) {
                throw new BadCredentialsException("终端信息不存在");
            }

            // 6. 创建认证主体
            TerminalPrincipal principal = createTerminalPrincipal(account, terminalInfo);

            // 7. 更新最后登录信息
            terminalRepository.updateLastLogin(principal.getTid(), getClientIp(authentication));

            // 8. 缓存认证结果（包含完整的principal信息）
            cacheAuthenticationResult(account, terminalInfo);

            log.info("终端认证成功: accountName={}, tid={}, terminalName={}",
                accountName, account.getTid(), terminalInfo.getTerminalName());

            return new UsernamePasswordAuthenticationToken(principal, password, principal.getAuthorities());

        } catch (AuthenticationException e) {
            // 重新抛出认证异常
            throw e;
        } catch (Exception e) {
            log.error("终端认证过程异常: accountName={}", accountName, e);
            throw new BadCredentialsException("认证服务异常");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 创建终端认证主体
     */
    private TerminalPrincipal createTerminalPrincipal(TerminalAccount account, TerminalInfo terminalInfo) {
        TerminalPrincipal principal = new TerminalPrincipal();
        principal.setTid(account.getTid());
        principal.setTerminalName(terminalInfo.getTerminalName());
        principal.setOid(terminalInfo.getOid());
        principal.setStatus(account.getStatus());
        return principal;
    }

    /**
     * 检查认证缓存
     * 
     * @param account 账号名
     * @param password 密码（用于验证缓存的有效性）
     * @return 缓存的认证主体，如果缓存未命中或无效则返回null
     */
    private TerminalPrincipal checkAuthenticationCache(String account, String password) {
        try {
            String cacheKey = AUTH_CACHE_PREFIX + account;
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            
            if (cachedData == null) {
                return null; // 缓存未命中
            }
            
            // 解析缓存数据 (格式: password_hash|tid|terminalName|oid)
            String[] parts = cachedData.split("\\|");
            if (parts.length != 4) {
                log.warn("认证缓存数据格式错误: account={}", account);
                redisTemplate.delete(cacheKey); // 删除无效缓存
                return null;
            }
            
            String cachedPasswordHash = parts[0];
            
            // 验证密码是否匹配
            if (!passwordEncoder.matches(password, cachedPasswordHash)) {
                return null; // 密码不匹配，缓存失效
            }
            
            // 构建TerminalPrincipal
            TerminalPrincipal principal = new TerminalPrincipal();
            principal.setTid(Long.parseLong(parts[1]));
            principal.setTerminalName(parts[2]);
            principal.setOid(Long.parseLong(parts[3]));
            
            return principal;
            
        } catch (Exception e) {
            log.warn("检查认证缓存异常: account={}", account, e);
            return null;
        }
    }

    /**
     * 缓存认证结果
     * 
     * @param account 账号信息
     * @param terminalInfo 终端信息
     */
    private void cacheAuthenticationResult(TerminalAccount account, TerminalInfo terminalInfo) {
        try {
            String cacheKey = AUTH_CACHE_PREFIX + account.getAccount();
            
            // 构建缓存数据 (格式: password_hash|tid|terminalName|oid)
            String cacheValue = String.join("|",
                account.getPassword(), // 已加密的密码
                account.getTid().toString(),
                terminalInfo.getTerminalName(),
                terminalInfo.getOid().toString()
            );
            
            // 缓存30分钟
            redisTemplate.opsForValue().set(cacheKey, cacheValue, 30, TimeUnit.MINUTES);
            
            log.debug("认证结果已缓存: account={}, tid={}, terminalName={}", account.getAccount(), terminalInfo.getTid(), terminalInfo.getTerminalName());
            
        } catch (Exception e) {
            log.warn("缓存认证结果失败: account={}", account, e);
        }
    }


    /**
     * 获取客户端IP地址
     */
    private String getClientIp(Authentication authentication) {
        try {
            Object details = authentication.getDetails();
            if (details != null && details.toString().contains("ip=")) {
                // 从认证详情中提取IP（需要配合Filter实现）
                String detailsStr = details.toString();
                int start = detailsStr.indexOf("ip='") + 4;
                int end = detailsStr.indexOf("'", start);
                if (start > 3 && end > start) {
                    return detailsStr.substring(start, end);
                }
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
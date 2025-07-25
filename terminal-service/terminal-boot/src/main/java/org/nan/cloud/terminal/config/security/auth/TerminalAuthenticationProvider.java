package org.nan.cloud.terminal.config.security.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.terminal.infrastructure.entity.auth.TerminalAccountDO;
import org.nan.cloud.terminal.infrastructure.mapper.auth.TerminalAccountMapper;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.entity.TerminalInfoDO;
import org.nan.cloud.terminal.infrastructure.persistence.mysql.repository.TerminalInfoRepository;
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

    private final TerminalAccountMapper terminalAccountMapper;
    private final TerminalInfoRepository terminalInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String account = authentication.getName();
        String password = (String) authentication.getCredentials();

        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new BadCredentialsException("用户名和密码不能为空");
        }


        try {
            // 1. 先检查Redis缓存
            TerminalPrincipal cachedPrincipal = checkAuthenticationCache(account, password);
            if (cachedPrincipal != null) {
                log.debug("终端认证缓存命中: account={}, tid={}", account, cachedPrincipal.getTid());
                return new UsernamePasswordAuthenticationToken(cachedPrincipal, password, cachedPrincipal.getAuthorities());
            }

            // 2. 缓存未命中，查询数据库进行完整认证
            TerminalAccountDO accountDO = findTerminalAccount(account);
            if (accountDO == null) {
                throw new BadCredentialsException("账号不存在");
            }

            // 3. 验证密码
            if (!passwordEncoder.matches(password, accountDO.getPassword())) {
                throw new BadCredentialsException("密码错误");
            }

            // 4. 检查账号状态
            if (accountDO.getStatus() != 0) {
                throw new DisabledException("账号已被禁用");
            }

            // 5. 查询终端详细信息
            TerminalInfoDO terminalInfo = terminalInfoRepository.getInfoByTid(accountDO.getTid());
            if (terminalInfo == null) {
                throw new BadCredentialsException("终端信息不存在");
            }

            // 6. 创建认证主体
            TerminalPrincipal principal = createTerminalPrincipal(accountDO, terminalInfo);

            // 7. 更新最后登录信息
            updateLastLoginInfo(accountDO, getClientIp(authentication));

            // 8. 缓存认证结果（包含完整的principal信息）
            cacheAuthenticationResult(account, password, principal);

            log.info("终端认证成功: account={}, tid={}, terminalName={}", 
                account, accountDO.getTid(), terminalInfo.getTerminalName());

            return new UsernamePasswordAuthenticationToken(principal, password, principal.getAuthorities());

        } catch (AuthenticationException e) {
            // 重新抛出认证异常
            throw e;
        } catch (Exception e) {
            log.error("终端认证过程异常: account={}", account, e);
            throw new BadCredentialsException("认证服务异常");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * 查询终端账号信息
     * 根据账号名称查询，这里需要根据实际数据库字段调整
     */
    private TerminalAccountDO findTerminalAccount(String account) {
        try {
            // 这里假设账号字段对应account，您可能需要根据实际情况调整查询方法
            return terminalAccountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TerminalAccountDO>()
                    .eq(TerminalAccountDO::getAccount, account)
                    .eq(TerminalAccountDO::getDeleted, false)
            );
        } catch (Exception e) {
            log.error("查询终端账号异常: account={}", account, e);
            return null;
        }
    }

    /**
     * 创建终端认证主体
     */
    private TerminalPrincipal createTerminalPrincipal(TerminalAccountDO accountDO, TerminalInfoDO terminalInfo) {
        TerminalPrincipal principal = new TerminalPrincipal();
        principal.setTid(accountDO.getTid());
        principal.setTerminalName(terminalInfo.getTerminalName());
        principal.setOid(terminalInfo.getOid());
        principal.setTgid(terminalInfo.getTgid());
        principal.setStatus(accountDO.getStatus());
        return principal;
    }

    /**
     * 更新最后登录信息
     */
    private void updateLastLoginInfo(TerminalAccountDO accountDO, String clientIp) {
        try {
            LocalDateTime now = LocalDateTime.now();
            terminalAccountMapper.updateLastLogin(
                accountDO.getTid(),
                now,
                clientIp,
                now
            );
        } catch (Exception e) {
            log.warn("更新最后登录信息失败: tid={}", accountDO.getTid(), e);
        }
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
            
            // 解析缓存数据 (格式: password_hash|tid|terminalName|oid|tgid|status)
            String[] parts = cachedData.split("\\|");
            if (parts.length != 6) {
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
            principal.setTgid(Long.parseLong(parts[4]));
            principal.setStatus(Integer.parseInt(parts[5]));
            
            return principal;
            
        } catch (Exception e) {
            log.warn("检查认证缓存异常: account={}", account, e);
            return null;
        }
    }

    /**
     * 缓存认证结果
     * 
     * @param account 账号名
     * @param password 原始密码
     * @param principal 认证主体
     */
    private void cacheAuthenticationResult(String account, String password, TerminalPrincipal principal) {
        try {
            String cacheKey = AUTH_CACHE_PREFIX + account;
            
            // 获取密码哈希（需要从数据库重新获取，因为principal中不包含密码）
            TerminalAccountDO accountDO = findTerminalAccount(account);
            if (accountDO == null) {
                return;
            }
            
            // 构建缓存数据 (格式: password_hash|tid|terminalName|oid|tgid|status)
            String cacheValue = String.join("|",
                accountDO.getPassword(), // 已加密的密码
                principal.getTid().toString(),
                principal.getTerminalName() != null ? principal.getTerminalName() : "",
                principal.getOid().toString(),
                principal.getTgid().toString(),
                principal.getStatus().toString()
            );
            
            // 缓存30分钟
            redisTemplate.opsForValue().set(cacheKey, cacheValue, 30, TimeUnit.MINUTES);
            
            log.debug("认证结果已缓存: account={}, tid={}", account, principal.getTid());
            
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
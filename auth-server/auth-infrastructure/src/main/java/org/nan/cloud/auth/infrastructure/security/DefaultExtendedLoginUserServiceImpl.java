package org.nan.cloud.auth.infrastructure.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * 用户名 + 组织代码登录
 */
public class DefaultExtendedLoginUserServiceImpl implements ExtendedLoginUserService{

    public static final String USERNAME_PARAMETER = "username";
    public static final String PASSWORD_PARAMETER = "password";
    public static final String ORG_SUFFIX_PARAMETER = "suffix";

    public static final String HASH = "#";

    /* 使用spring security userDetailService来实现 */
    private UserDetailsService userDetailsService;

    private PasswordEncoder passwordEncoder;

    public DefaultExtendedLoginUserServiceImpl(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserPrincipal loadUserByAuthParams(Map<String, String> authParams) throws UsernameNotFoundException {
        if (!authParams.containsKey(USERNAME_PARAMETER) || !authParams.containsKey(PASSWORD_PARAMETER) || !authParams.containsKey(ORG_SUFFIX_PARAMETER)) {
            throw new UsernameNotFoundException("Invalid params-need username,password,suffix");
        }
        String username = authParams.get(USERNAME_PARAMETER);
        String suffix = authParams.get(ORG_SUFFIX_PARAMETER);
        String comboUsername = username + HASH + suffix;
        return (UserPrincipal) userDetailsService.loadUserByUsername(comboUsername);
    }

    @Override
    public void authenticateUser(Map<String, String> authParams, UserPrincipal userPrincipal) throws AuthenticationException {
        if (!passwordEncoder.matches(authParams.get(PASSWORD_PARAMETER), userPrincipal.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
    }
}

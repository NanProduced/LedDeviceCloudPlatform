package org.nan.cloud.auth.infrastructure.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

public interface ExtendedLoginUserService {

    UserPrincipal loadUserByAuthParams(Map<String, String> authParams) throws UsernameNotFoundException;

    void authenticateUser(Map<String, String> authParams, UserPrincipal userPrincipal) throws AuthenticationException;

}

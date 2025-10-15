package org.nan.cloud.auth.boot.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录拓展-认证过滤器
 */
public class ExtendedLoginAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    private boolean postOnly = true;

    public ExtendedLoginAuthenticationProcessingFilter(String loginProcessesUrl, AuthenticationManager authenticationManager) {
        super(new AntPathRequestMatcher(loginProcessesUrl, "POST"), authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        // 仅支持POST请求
        if (this.postOnly && !request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }
        ExtendedLoginAuthenticationToken authRequest = new ExtendedLoginAuthenticationToken(getAuthParams(request));
        setDetails(request, authRequest);
        return getAuthenticationManager().authenticate(authRequest);
    }

    private Map<String, String> getAuthParams(HttpServletRequest httpServletRequest) {
        Map<String, String[]> originRequestMap = httpServletRequest.getParameterMap();
        Assert.notEmpty(originRequestMap, "Login params should not be empty");
        // 转换参数
        Map<String, String> convertRequestParams = new HashMap<>(originRequestMap.size());
        for (Map.Entry<String, String[]> entry : originRequestMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            if (null == paramValues || paramValues.length == 0 || !StringUtils.hasText(paramValues[0])) {
                continue;
            }
            convertRequestParams.put(paramName, paramValues[0]);
        }
        return convertRequestParams;
    }

    // 记录Http请求上下文信息
    protected void setDetails(HttpServletRequest request, ExtendedLoginAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }


}

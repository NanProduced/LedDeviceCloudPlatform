package org.nan.cloud.auth.boot.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.common.utils.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class ExtendedLoginJsonRespAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache;

    public ExtendedLoginJsonRespAuthenticationSuccessHandler(RequestCache requestCache) {
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        log.debug("Extended Login success - username: {}", authentication.getPrincipal());
        // 清理认证异常
        clearAuthenticationAttributes(request);

        String redirectUri = Optional.ofNullable(requestCache.getRequest(request, response))
                .map(SavedRequest::getRedirectUrl)
                .orElse("/");

        // 是否为 AJAX/JSON 请求
        String xrw = request.getHeader("X-Requested-With");
        String accept =  request.getHeader("Accept");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(xrw) ||
                (null != accept && accept.contains("application/json"));

        if (isAjax) {
            String respJson = JsonUtils.toJson(ExtendedLoginRespJson.builder()
                    .code("200")
                    .redirectUri(redirectUri)
                    .build());
            log.debug("Extended login success - response: {}", respJson);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json;charset=utf-8");
            response.setContentLength(respJson.getBytes(StandardCharsets.UTF_8).length);
            response.getWriter().write(respJson);
        }
        else {
            // 在发重定向前先把上下文写入 session
            HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
            repo.saveContext(SecurityContextHolder.getContext(), request, response);
            // 浏览器请求执行重定向
            log.debug("Extended login success (Browser) - redirect url: {}", redirectUri);
            response.sendRedirect(redirectUri);
        }
    }

    /**
     * Removes temporary authentication-related data which may have been stored in the
     * session during the authentication process.
     */
    protected final void clearAuthenticationAttributes(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }


}

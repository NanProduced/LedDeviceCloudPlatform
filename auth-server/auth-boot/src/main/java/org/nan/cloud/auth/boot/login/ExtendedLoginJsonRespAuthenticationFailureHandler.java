package org.nan.cloud.auth.boot.login;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.common.utils.JsonUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ExtendedLoginJsonRespAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

        log.debug("Extended login failure - exception: {}", exception.getMessage());
        String respJson = JsonUtils.toJson(ExtendedLoginRespJson.builder()
                .code("500")
                .msg(exception.getMessage())
                .build());
        log.debug("Extended login failure resp: {}", respJson);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json;charset=utf-8");
        response.setContentLength(respJson.getBytes(StandardCharsets.UTF_8).length);
        response.getWriter().write(respJson);
    }
}

package org.nan.cloud.auth.boot.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.nan.cloud.auth.infrastructure.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
@Slf4j
@RequiredArgsConstructor
public class PageController {

    private final RequestCache requestCache;

    private final OAuth2ServerProps oAuth2ServerProps;

    @GetMapping("/login")
    public String login(Model model, Authentication authentication, HttpServletRequest req, HttpServletResponse res) {
        if (null != authentication && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        // 用户第一次访问一个需要 OAuth2 授权的端点时，Spring 会把这次原始请求保存到 HttpSessionRequestCache/RequestCache 里；
        String clientTitle = Optional.ofNullable(requestCache.getRequest(req, res))
                .map(savedRequest -> savedRequest.getParameterValues(OAuth2ParameterNames.CLIENT_ID))
                .map(clientIds -> clientIds[0])
                .orElse(oAuth2ServerProps.getLoginPageTitle());
        model.addAttribute("title", clientTitle);
        return "/login";
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
        model.addAttribute("authentication", authentication);
        model.addAttribute("session_id", details.getSessionId());
        model.addAttribute("oid", userPrincipal.getOid());
        model.addAttribute("portalGatewayUrl", "http://192.168.1.222:8082/" );
        return "home";
    }

}

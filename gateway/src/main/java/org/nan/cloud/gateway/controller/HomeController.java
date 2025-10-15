package org.nan.cloud.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    @GetMapping("/")
    public Mono<String> home(
            Model model,
            @RegisteredOAuth2AuthorizedClient("gateway-server") OAuth2AuthorizedClient client,
            @AuthenticationPrincipal OidcUser user
    ) {
        // 已拿到 client 和 user
        model.addAttribute("username",  user.getName());
        model.addAttribute("uid",       user.getIdToken().getClaim("uid"));
        model.addAttribute("oid",       user.getIdToken().getClaim("oid"));
        model.addAttribute("ugid",      user.getIdToken().getClaim("ugid"));
        model.addAttribute("idToken",   user.getIdToken().getTokenValue());
        model.addAttribute("accessToken", client.getAccessToken().getTokenValue());
        model.addAttribute("refreshToken", Objects.requireNonNullElse(client.getRefreshToken().getTokenValue(), "未签发Refresh_Token"));
        return Mono.just("home");
    }

    @GetMapping("/logout_status")
    public Mono<String> logout_status(Model model) {
        return Mono.just("logout_status");
    }


}

package org.nan.cloud.auth.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/internal/auth")
public interface AuthApi {

    @PostMapping("/encode")
    String encodePsw(@RequestParam("psw") String password);
}

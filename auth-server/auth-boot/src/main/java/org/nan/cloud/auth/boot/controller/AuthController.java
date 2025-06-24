package org.nan.cloud.auth.boot.controller;

import lombok.RequiredArgsConstructor;
import org.nan.cloud.auth.api.AuthApi;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encodePsw(String password) {
        return passwordEncoder.encode(password);
    }
}

package org.nan.cloud.auth.boot.controller;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommonController {

    private final JWKSource<SecurityContext> jwkSource;

    @GetMapping("/rsa/publicKey")
    public Map<String,Object> publicKey() throws Exception {
        JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
        List<JWK> jwks = jwkSource.get(selector, null);
        JWKSet jwkSet = new JWKSet(jwks);
        return jwkSet.toJSONObject();
    }
}

package org.nan.cloud.auth.boot.oidc;

import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.nan.cloud.auth.boot.utils.Jwks;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;

@Slf4j
public class OidcEndSessionSingleLogoutSuccessHandler implements LogoutSuccessHandler {

    private  RegisteredClientRepository registeredClientRepository;

    private  OAuth2AuthorizationService authorizationService;

    private  OAuth2ServerProps oAuth2ServerProps;

    /**
     * RSA密钥
     */
    private RSAKey rsaJWK;
    /**
     * JWS签名对象（根据RSAKey.privateKey生成）
     */
    private JWSSigner signer;

    /**
     * 构造函数
     */
    @SneakyThrows
    public OidcEndSessionSingleLogoutSuccessHandler(RegisteredClientRepository registeredClientRepository,
                                                    OAuth2AuthorizationService authorizationService,
                                                    OAuth2ServerProps oAuth2ServerProps) {
        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.oAuth2ServerProps = oAuth2ServerProps;

        // Create RSA-signer with the private key
        rsaJWK = Jwks.convertRsaKey(this.oAuth2ServerProps);
        signer = new RSASSASigner(rsaJWK);
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    }
}

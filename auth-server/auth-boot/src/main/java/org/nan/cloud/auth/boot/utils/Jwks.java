package org.nan.cloud.auth.boot.utils;


import com.nimbusds.jose.jwk.RSAKey;
import org.nan.cloud.auth.boot.config.OAuth2ServerProps;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

public final class Jwks {

    public static final String DEFAULT_JWK_ALGORITHM = "RSA";

    private Jwks() {
    }

    /**
     * 生成RSA秘钥对
     *
     * @return RSA秘钥对
     */
    public static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    public static RSAKey convertRsaKey(OAuth2ServerProps oAuth2ServerProps) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        RSAKey rsaKey = null;
        if (null == oAuth2ServerProps || null == oAuth2ServerProps.getRsaPublicKey() || null == oAuth2ServerProps.getRsaPrivateKey()) {
            rsaKey = Jwks.generateRsa();
        } else {
            rsaKey = new RSAKey.Builder(Jwks.convertRsaPublicKey(oAuth2ServerProps.getRsaPublicKey()))
                    .privateKey(Jwks.convertRsaPrivateKey(oAuth2ServerProps.getRsaPrivateKey()))
                    .build();
        }
        return rsaKey;
    }

    public static RSAPublicKey convertRsaPublicKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String resourceText = readResourceText(resource);
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance(DEFAULT_JWK_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(getPublicKeySpec(resourceText)));
        return publicKey;
    }

    public static RSAPrivateKey convertRsaPrivateKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String resourceText = readResourceText(resource);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance(DEFAULT_JWK_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(getPrivateKeySpec(resourceText)));
        return privateKey;
    }

    public static String readResourceText(Resource resourceLocation) throws IOException {
        if (null == resourceLocation || !resourceLocation.exists()) {
            return null;
        }
        try (InputStream inputStream = resourceLocation.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private static byte[] getPublicKeySpec(String keyText) {
        keyText = keyText.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        return Base64.getMimeDecoder().decode(keyText);
    }

    private static byte[] getPrivateKeySpec(String keyText) {
        keyText = keyText.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        return Base64.getMimeDecoder().decode(keyText);
    }
}

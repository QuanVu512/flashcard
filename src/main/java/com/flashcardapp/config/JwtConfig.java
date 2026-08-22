package com.flashcardapp.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class JwtConfig {

    private static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String GOOGLE_LINK_TOKEN_TYPE = "google_link";

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(properties))
                .macAlgorithm(JWT_ALGORITHM)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                tokenTypeValidator(ACCESS_TOKEN_TYPE)
        ));
        return decoder;
    }

    @Bean("googleLinkJwtDecoder")
    JwtDecoder googleLinkJwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(properties))
                .macAlgorithm(JWT_ALGORITHM)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                tokenTypeValidator(GOOGLE_LINK_TOKEN_TYPE)
        ));
        return decoder;
    }

    private OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> tokenTypeValidator(String expectedType) {
        return jwt -> expectedType.equals(jwt.getClaimAsString("token_type"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "Token type is not accepted for this endpoint",
                        null
                ));
    }

    private SecretKey signingKey(JwtProperties properties) {
        byte[] keyBytes;
        if (StringUtils.hasText(properties.getBase64Secret())) {
            try {
                keyBytes = Base64.getDecoder().decode(properties.getBase64Secret());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("JWT_BASE64_SECRET phải đúng định dạng base64", exception);
            }
        } else if (StringUtils.hasText(properties.getSecret())) {
            keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException("Cần cấu hình JWT_BASE64_SECRET hoặc JWT_SECRET");
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret cần tối thiểu 32 bytes cho HS256");
        }
        return new SecretKeySpec(keyBytes, JWT_ALGORITHM.getName());
    }
}

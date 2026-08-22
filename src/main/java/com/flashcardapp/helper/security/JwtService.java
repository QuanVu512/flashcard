package com.flashcardapp.helper.security;

import com.flashcardapp.config.AuthSessionProperties;
import com.flashcardapp.entity.AppUser;
import com.flashcardapp.entity.AuthMethod;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final AuthSessionProperties sessionProperties;

    public JwtService(JwtEncoder jwtEncoder,
                      @Qualifier("googleLinkJwtDecoder") JwtDecoder jwtDecoder,
                      AuthSessionProperties sessionProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.sessionProperties = sessionProperties;
    }

    public String generateAccessToken(UserDetails userDetails, AuthMethod authMethod) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("token_type", "access")
                .claim("auth_method", authMethod.name())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256)
                        .type("JWT")
                        .build(),
                claims
        )).getTokenValue();
    }

    public String generateGoogleLinkToken(AppUser user,
                                          String issuer,
                                          String subject,
                                          String googleEmail,
                                          String displayName) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600))
                .claim("token_type", "google_link")
                .claim("google_issuer", issuer)
                .claim("google_subject", subject)
                .claim("google_email", googleEmail)
                .claim("google_name", displayName == null ? "" : displayName)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
                claims
        )).getTokenValue();
    }

    public GoogleLinkClaims readGoogleLinkToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        if (!"google_link".equals(jwt.getClaimAsString("token_type"))) {
            throw new IllegalArgumentException("Phiên liên kết Google không hợp lệ");
        }
        return new GoogleLinkClaims(
                jwt.getSubject(),
                jwt.getClaimAsString("google_issuer"),
                jwt.getClaimAsString("google_subject"),
                jwt.getClaimAsString("google_email"),
                jwt.getClaimAsString("google_name")
        );
    }

    public long expirationSeconds() {
        return sessionProperties.accessDuration().toSeconds();
    }

    public record GoogleLinkClaims(
            String localEmail,
            String issuer,
            String subject,
            String googleEmail,
            String displayName
    ) {
    }
}

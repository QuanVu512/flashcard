package com.flashcardapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;

@ConfigurationProperties(prefix = "app.auth.google")
public class GoogleAuthProperties {

    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String redirectUri = "{baseUrl}/login/oauth2/code/google";
    private String frontendBaseUrl;

    public void validateEnabledConfiguration() {
        if (!enabled) return;
        if (!StringUtils.hasText(clientId)
                || !StringUtils.hasText(clientSecret)
                || !StringUtils.hasText(redirectUri)) {
            throw new IllegalStateException(
                    "Google Login cần GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET và GOOGLE_REDIRECT_URI"
            );
        }
        validatedFrontendBaseUrl();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String frontendUrl(String localPath) {
        if (!StringUtils.hasText(frontendBaseUrl)) return localPath;
        return validatedFrontendBaseUrl() + localPath;
    }

    private String validatedFrontendBaseUrl() {
        if (!StringUtils.hasText(frontendBaseUrl)) return "";
        String value = frontendBaseUrl.trim().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_FRONTEND_BASE_URL không hợp lệ", exception);
        }
        boolean validScheme = "https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme());
        if (!validScheme
                || !StringUtils.hasText(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (StringUtils.hasText(uri.getPath()) && !"/".equals(uri.getPath()))) {
            throw new IllegalStateException("APP_FRONTEND_BASE_URL phải là origin HTTP/HTTPS, không kèm path");
        }
        return value;
    }
}

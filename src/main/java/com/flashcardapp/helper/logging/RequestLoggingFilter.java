package com.flashcardapp.helper.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String RENDER_REQUEST_ID_HEADER = "Rndr-Id";
    private static final int MAX_USER_AGENT_LENGTH = 120;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String previousRequestId = MDC.get("requestId");
        String previousUsername = MDC.get("username");
        String requestId = resolveRequestId(request);
        long startedAt = System.nanoTime();

        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("username", resolveUsername());
            writeRequestLog(request, response, startedAt);
            restoreMdcValue("requestId", previousRequestId);
            restoreMdcValue("username", previousUsername);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico");
    }

    private void writeRequestLog(HttpServletRequest request, HttpServletResponse response, long startedAt) {
        long responseTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        int status = response.getStatus();
        String message = "HTTP {} {} status={} responseTimeMs={} ip={} queryPresent={} requestBytes={} userAgent=\"{}\"";
        Object[] arguments = {
                request.getMethod(),
                request.getRequestURI(),
                status,
                responseTimeMs,
                resolveClientIp(request),
                StringUtils.hasText(request.getQueryString()),
                request.getContentLengthLong(),
                summarizeUserAgent(request.getHeader("User-Agent"))
        };

        if (status >= 500) {
            LOGGER.error(message, arguments);
        } else if (status >= 400) {
            LOGGER.warn(message, arguments);
        } else {
            LOGGER.info(message, arguments);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = firstValidRequestId(request.getHeader(REQUEST_ID_HEADER));
        if (requestId != null) {
            return requestId;
        }

        requestId = firstValidRequestId(request.getHeader(RENDER_REQUEST_ID_HEADER));
        if (requestId != null) {
            return requestId;
        }

        return UUID.randomUUID().toString();
    }

    private String firstValidRequestId(String requestId) {
        if (StringUtils.hasText(requestId)
                && requestId.length() <= 80
                && requestId.matches("[A-Za-z0-9._-]+")) {
            return requestId;
        }

        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        return authentication.getName();
    }

    private String summarizeUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "-";
        }

        String cleaned = userAgent.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (cleaned.length() <= MAX_USER_AGENT_LENGTH) {
            return cleaned;
        }

        return cleaned.substring(0, MAX_USER_AGENT_LENGTH - 3) + "...";
    }

    private void restoreMdcValue(String key, String value) {
        if (value == null) {
            MDC.remove(key);
            return;
        }

        MDC.put(key, value);
    }
}

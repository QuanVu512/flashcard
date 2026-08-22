package com.flashcardapp.helper.security;

import com.flashcardapp.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(SecurityProperties.DEFAULT_FILTER_ORDER + 2)
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_API_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/otp/verify",
            "/api/auth/otp/resend",
            "/api/auth/google/link",
            "/api/translation/suggest",
            "/api/handwriting/recognize",
            "/api/games/score"
    );

    private final RateLimitProperties properties;
    private final ConcurrentMap<String, WindowState> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();

    public ApiRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String key = limitKey(request);
        WindowState state = incrementWindow(key, now);
        if (state.count() > properties.apiCapacityOrDefault()) {
            writeTooManyRequests(response, state.resetAtMillis(), now);
            return;
        }

        cleanupOccasionally(now);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabledOrDefault() || !LIMITED_API_PATHS.contains(request.getRequestURI());
    }

    private WindowState incrementWindow(String key, long now) {
        long windowMillis = properties.windowSecondsOrDefault() * 1000L;
        return windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtMillis()) {
                return new WindowState(now + windowMillis, 1);
            }
            return new WindowState(current.resetAtMillis(), current.count() + 1);
        });
    }

    private String limitKey(HttpServletRequest request) {
        return request.getMethod() + ":" + request.getRequestURI() + ":" + principalOrIp(request);
    }

    private String principalOrIp(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && StringUtils.hasText(authentication.getName())) {
            return authentication.getName().toLowerCase();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupOccasionally(long now) {
        if (requestCounter.incrementAndGet() % 500 != 0) {
            return;
        }
        windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtMillis());
    }

    private void writeTooManyRequests(HttpServletResponse response, long resetAtMillis, long now) throws IOException {
        long retryAfterSeconds = Math.max(1, (resetAtMillis - now + 999) / 1000);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("""
                {
                  "message": "Bạn thao tác hơi nhanh, hãy thử lại sau một chút.",
                  "status": 429,
                  "details": ["API đang được giới hạn để bảo vệ tài khoản và tài nguyên hệ thống."],
                  "timestamp": "%s"
                }
                """.formatted(OffsetDateTime.now()));
    }

    private record WindowState(long resetAtMillis, int count) {
    }
}

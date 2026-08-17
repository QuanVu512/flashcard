package com.flashcardapp.helper.exception;

import com.flashcardapp.helper.path.AppPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (wantsHtml(request)) {
            response.sendRedirect(AppPath.LOGIN + "?expired");
            return;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {
                  "message": "Bạn cần đăng nhập để tiếp tục.",
                  "status": 401,
                  "details": ["Phiên đăng nhập bị thiếu, hết hạn hoặc không hợp lệ."],
                  "timestamp": "%s"
                }
                """.formatted(OffsetDateTime.now()));
    }

    private boolean wantsHtml(HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/")) {
            return false;
        }
        String accept = request.getHeader("Accept");
        return accept == null || accept.contains(MediaType.TEXT_HTML_VALUE);
    }
}

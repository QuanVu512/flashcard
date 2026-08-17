package com.flashcardapp.helper.exception;

import com.flashcardapp.helper.path.AppPath;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (wantsHtml(request)) {
            response.sendRedirect(AppPath.ACCESS_DENIED);
            return;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {
                  "message": "Bạn không có quyền thực hiện thao tác này.",
                  "status": 403,
                  "details": ["Tài khoản đã đăng nhập nhưng chưa có quyền phù hợp."],
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

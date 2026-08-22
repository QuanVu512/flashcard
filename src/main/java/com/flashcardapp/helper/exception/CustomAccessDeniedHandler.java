package com.flashcardapp.helper.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
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
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        boolean csrfFailure = accessDeniedException instanceof CsrfException;
        String message = csrfFailure
                ? "Yêu cầu bảo mật không hợp lệ."
                : "Bạn không có quyền thực hiện thao tác này.";
        String details = csrfFailure
                ? "CSRF token bị thiếu, hết hạn hoặc không khớp."
                : "Tài khoản đã đăng nhập nhưng chưa có quyền phù hợp.";
        response.getWriter().write("""
                {
                  "message": "%s",
                  "status": 403,
                  "details": ["%s"],
                  "timestamp": "%s"
                }
                """.formatted(message, details, OffsetDateTime.now()));
    }
}

# Security Notes

Flashcard dùng Spring Security theo mô hình server-rendered MVC, nên cơ chế chính là session cookie thay vì JWT. Cách này phù hợp với Thymeleaf vì frontend và backend chạy cùng một app.

## Những lớp bảo vệ chính

- Authentication bằng form login, mật khẩu hash bằng BCrypt.
- Authorization theo role: user thường dùng thư viện học tập, admin truy cập `/admin/**`.
- Method security với `@PreAuthorize` cho controller admin.
- Ownership guard ở tầng service: user chỉ lấy được folder/set thuộc `Client` của chính mình.
- CSRF bật mặc định cho form và các API ghi dữ liệu.
- Session hardening: session fixation protection, session timeout, HttpOnly cookie, SameSite cookie và Secure cookie trong production.
- Security headers: Content Security Policy, frame deny, same-origin referrer policy và Permissions-Policy.
- REST error handler trả JSON cho lỗi API thay vì lộ stacktrace.
- Rate limit cho `/api/translation/suggest`, `/api/handwriting/recognize` và `/api/games/score`.
- Production profile ẩn message, binding error và stacktrace khỏi response.

## Admin bootstrap

Tài khoản admin được tạo hoặc promote khi app khởi động nếu có đủ biến môi trường:

```properties
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-strong-password
ADMIN_DISPLAY_NAME=Admin
```

Không commit giá trị thật của các biến này.

## Biến môi trường nên đặt khi deploy

```properties
SPRING_PROFILES_ACTIVE=prod
SESSION_COOKIE_SECURE=true
SESSION_COOKIE_SAME_SITE=Lax
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_API_CAPACITY=120
APP_RATE_LIMIT_WINDOW_SECONDS=60
```

Nếu sau này frontend tách domain riêng, chỉ mở CORS cho đúng domain:

```properties
APP_CORS_ALLOWED_ORIGINS=https://your-frontend.example.com
```

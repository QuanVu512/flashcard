# Security Model

Flashcard dùng Spring Security theo mô hình REST stateless. Spring OAuth2 Resource Server xác thực JWT, còn trình duyệt nhận access token qua cookie bảo mật thay vì để JavaScript quản lý token.

## Luồng xác thực

1. Frontend lấy CSRF token từ `GET /api/auth/csrf`.
2. Frontend gửi thông tin đăng nhập đến `POST /api/auth/login` hoặc `POST /api/auth/register`.
3. Backend xác thực mật khẩu bằng BCrypt và ký JWT HS256.
4. JWT được trả qua header `Set-Cookie` với `HttpOnly`, `SameSite`, `Path=/` và `Secure` trong production.
5. `CookieBearerTokenResolver` lấy JWT từ cookie; OAuth2 Resource Server kiểm tra chữ ký, thời hạn và role.
6. Frontend gọi `GET /api/auth/me` khi tải trang để khôi phục thông tin hiển thị của phiên.
7. `POST /api/auth/logout` trả cookie hết hạn để xoá JWT khỏi trình duyệt.

JWT không xuất hiện trong response JSON và không được lưu trong `localStorage` hoặc `sessionStorage`. API client tin cậy vẫn có thể gửi Bearer token trong header `Authorization`.

## CSRF

Vì trình duyệt tự gửi cookie xác thực, các method thay đổi dữ liệu được bảo vệ bằng CSRF double-submit cookie. Frontend đọc CSRF token riêng và gửi lại qua `X-XSRF-TOKEN`. CSRF token không phải credential đăng nhập.

Các method an toàn (`GET`, `HEAD`, `OPTIONS`, `TRACE`) không yêu cầu CSRF token. Login, register và logout vẫn yêu cầu CSRF vì đều là request ghi dữ liệu. Request dùng Bearer token rõ ràng trong header không cần CSRF vì trình duyệt không tự gắn credential này.

## Phân quyền

- `/api/auth/login`, `/api/auth/register`, `/api/auth/logout` và `/api/auth/csrf` là endpoint công khai.
- Các endpoint `/api/**` còn lại yêu cầu JWT hợp lệ.
- `/api/admin/**` yêu cầu `ROLE_ADMIN` và được bảo vệ thêm bằng method security.
- Service kiểm tra quyền sở hữu folder và flashcard set, tránh truy cập dữ liệu của người dùng khác chỉ bằng cách thay UUID.

## Cấu hình cookie

```properties
AUTH_COOKIE_NAME=flashcard_access_token
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
```

- Local HTTP dùng `AUTH_COOKIE_SECURE=false`.
- Production HTTPS dùng `AUTH_COOKIE_SECURE=true`.
- Nếu frontend nằm ở site khác, dùng `SameSite=None`, bắt buộc bật `Secure` và chỉ định CORS origin chính xác.
- Không đặt `Domain`, vì vậy cookie mặc định là host-only.

## JWT secret

Production bắt buộc cấu hình một trong hai biến:

```properties
JWT_SECRET=random-secret-at-least-32-bytes
# hoặc
JWT_BASE64_SECRET=base64-encoded-random-secret
```

Không commit secret thật. Profile `prod` không dùng secret phát triển mặc định.

## Các lớp bảo vệ bổ sung

- Content Security Policy, frame deny, same-origin referrer policy và Permissions Policy.
- JSON error response cho 401/403 và lỗi API.
- Rate limit cho login, register, dịch, OCR và ghi điểm để bảo vệ tài khoản/quota.
- Production ẩn stack trace và binding details khỏi response.
- CORS credential chỉ mở cho origin được cấu hình, không dùng wildcard.

## OAuth2 mở rộng

Hiện tại ứng dụng tự phát hành JWT và dùng Spring OAuth2 Resource Server để xác thực. Dependency OAuth2 Client là nền tảng cho đăng nhập Google trong tương lai, nhưng provider login chưa được bật mặc định.

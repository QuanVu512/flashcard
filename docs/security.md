# Security Model

Flashcard dùng Spring Security với hai phương thức đăng nhập: Google OpenID Connect và email/mật khẩu có OTP. Phiên ứng dụng vẫn do backend phát hành; Google không thay thế database người dùng và không giữ dữ liệu flashcard.

## Phiên đăng nhập

1. Access token là JWT HS256 sống ngắn, mặc định 15 phút.
2. Refresh token là chuỗi ngẫu nhiên, chỉ bản hash SHA-256 được lưu trong `auth_refresh_sessions`.
3. Mỗi lần refresh, token cũ bị thu hồi và token mới giữ nguyên thời điểm hết hạn tuyệt đối.
4. Phiên mật khẩu hết hạn sau 3 ngày; phiên bắt đầu từ Google hết hạn sau 30 ngày.
5. Token được gửi bằng cookie `HttpOnly`; JavaScript không đọc token và không lưu credential trong `localStorage` hoặc `sessionStorage`.
6. Spring OAuth2 Resource Server chỉ chấp nhận JWT có claim `token_type=access`. Token tạm dùng để liên kết Google không thể thay cho access token.

HTTP session chỉ được tạo tạm thời để Spring Security lưu OAuth authorization request trong lúc chuyển hướng sang Google. Session này bị vô hiệu ngay khi callback thành công hoặc thất bại.

## Đăng nhập email và OTP

1. Mật khẩu được kiểm tra bằng BCrypt.
2. Email chưa xác minh luôn phải hoàn tất OTP trước khi được cấp phiên.
3. Thiết bị chưa tin cậy phải nhập OTP sau khi mật khẩu đúng.
4. OTP gồm 6 chữ số, mặc định sống 5 phút, chỉ dùng một lần và bị khóa sau số lần nhập sai cấu hình.
5. Database chỉ lưu HMAC-SHA256 của OTP với secret riêng `AUTH_OTP_HASH_SECRET`; mã rõ không được ghi log.
6. Gửi lại OTP có thời gian chờ và endpoint được rate limit.

Checkbox **Nhớ thiết bị trong 30 ngày** tạo một cookie ngẫu nhiên `HttpOnly` và lưu bản hash trong `auth_trusted_devices`. Nó chỉ bỏ qua OTP cho đúng người dùng và User-Agent đó. Phiên mật khẩu vẫn hết hạn sau 3 ngày, vì vậy người dùng phải nhập lại mật khẩu nhưng không cần OTP trong thời gian thiết bị còn tin cậy.

## Google và bảo toàn tài khoản cũ

Google identity được định danh bằng bộ ba `provider + issuer + subject`, không chỉ bằng email.

- Identity đã liên kết đăng nhập đúng `user_id` cũ.
- Google email chưa tồn tại tạo người dùng mới với email đã xác minh.
- Google email trùng tài khoản local nhưng chưa liên kết không bị tự động gộp. Backend phát token liên kết tạm 10 phút; người dùng phải nhập đúng mật khẩu local rồi mới thêm identity Google.
- Vì liên kết giữ nguyên `users.id` và `client_id`, folder, flashcard set, điểm và dữ liệu hiện có không thay đổi.

## Cookie và CSRF

Các cookie xác thực gồm access token, refresh token, trusted device và token liên kết tạm. Tất cả đều có `HttpOnly`, `Path=/`, `SameSite` và `Secure` trong production.

```properties
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
```

Vì trình duyệt tự gửi cookie, các method thay đổi dữ liệu dùng CSRF token qua header `X-XSRF-TOKEN`. CSRF token không phải credential đăng nhập. Nếu frontend nằm khác site, dùng HTTPS, `SameSite=None`, `Secure=true` và CORS origin cụ thể; không kết hợp credential với wildcard origin.

## Secrets production

```properties
JWT_SECRET=random-secret-at-least-32-bytes
AUTH_OTP_HASH_SECRET=different-random-secret-at-least-32-bytes
```

Có thể dùng `JWT_BASE64_SECRET` thay cho `JWT_SECRET`. Hai secret JWT và OTP phải độc lập, không commit vào repository và không đặt trong frontend.

## Các lớp bảo vệ bổ sung

- Content Security Policy, frame deny, same-origin referrer policy và Permissions Policy.
- JSON error response thống nhất cho 400, 401, 403 và lỗi API.
- Rate limit cho login, register, OTP, liên kết Google và API tốn quota.
- Production ẩn stack trace và binding details.
- Quyền sở hữu dữ liệu được kiểm tra ở service; `/api/admin/**` yêu cầu `ROLE_ADMIN`.

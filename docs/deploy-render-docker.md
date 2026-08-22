# Deploy To Render With Docker

Project đã có `Dockerfile`, vì vậy trên Render chỉ cần tạo Web Service với runtime Docker.

## Required Environment Variables

```properties
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
JWT_SECRET=change-this-random-secret-at-least-32-chars
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=Lax
```

## Optional Azure Translator

Khai báo khi muốn bật gợi ý nghĩa/phiên âm:

```properties
TRANSLATION_PROVIDER=azure
TRANSLATION_DEFAULT_TARGET=vi
AZURE_TRANSLATOR_ENDPOINT=https://api.cognitive.microsofttranslator.com
AZURE_TRANSLATOR_KEY=...
AZURE_TRANSLATOR_REGION=global
```

## Optional Azure Vision OCR

Khai báo khi muốn bật bảng vẽ nhận dạng chữ viết:

```properties
HANDWRITING_PROVIDER=azure
AZURE_VISION_KEY=...
AZURE_VISION_ENDPOINT=https://your-vision-resource.cognitiveservices.azure.com
```

## Optional Admin Account

Khai báo nếu muốn app tự tạo hoặc promote tài khoản admin lúc khởi động:

```properties
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-strong-password
ADMIN_DISPLAY_NAME=Admin
```

## Optional Rate Limit

Mặc định app giới hạn các API tốn quota trong 1 phút. Có thể chỉnh trên Render:

```properties
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_API_CAPACITY=120
APP_RATE_LIMIT_WINDOW_SECONDS=60
```

## Render Notes

Render tự cấp biến `PORT`, Dockerfile sẽ để Spring Boot đọc port qua cấu hình ứng dụng.

Gói free có thể sleep khi không có request. Đây là hành vi bình thường và không phải lỗi app.

## Production Logs

Khi `SPRING_PROFILES_ACTIVE=prod`, app vẫn in log ra console để Render thu thập trong tab `Logs`.
Đây là nơi nên dùng để debug trên môi trường deploy vì log đi cùng service runtime của Render.

Profile `prod` cũng ghi rolling log vào `${LOG_DIR}/flashcard.log`, mặc định là `logs/flashcard.log`.
File này hữu ích nếu chạy trên VPS hoặc môi trường có ổ đĩa ổn định. Trên Render, filesystem của container không nên được xem là nơi lưu log dài hạn sau redeploy/restart.
Nếu cần lưu lâu hơn retention của Render, dùng Log Streams để đẩy log sang dịch vụ ngoài.

Log request của app có dạng:

```text
HTTP GET /library status=200 responseTimeMs=84 ip=... queryPresent=false requestBytes=-1 userAgent="..."
```

## Checklist Trước Khi Deploy

- Database Neon đã tạo và cho phép SSL.
- `DATABASE_URL` dùng dạng JDBC, có `sslmode=require`.
- Không commit file `.env`.
- Production phải để `SPRING_PROFILES_ACTIVE=prod`, khai báo `JWT_SECRET` thật và chỉ phục vụ qua HTTPS để dùng cookie `Secure`.
- Trên Render đã thêm key Translator/Vision nếu muốn dùng API ngoài.
- Nếu muốn dùng `/admin`, đã thêm `ADMIN_EMAIL` và `ADMIN_PASSWORD`.
- Local build qua Maven trước khi push.

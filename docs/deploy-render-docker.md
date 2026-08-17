# Deploy To Render With Docker

Project đã có `Dockerfile`, vì vậy trên Render chỉ cần tạo Web Service với runtime Docker.

## Required Environment Variables

```properties
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
SESSION_COOKIE_SECURE=true
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

## Checklist Trước Khi Deploy

- Database Neon đã tạo và cho phép SSL.
- `DATABASE_URL` dùng dạng JDBC, có `sslmode=require`.
- Không commit file `.env`.
- Production nên để `SPRING_PROFILES_ACTIVE=prod` và `SESSION_COOKIE_SECURE=true`.
- Trên Render đã thêm key Translator/Vision nếu muốn dùng API ngoài.
- Nếu muốn dùng `/admin`, đã thêm `ADMIN_EMAIL` và `ADMIN_PASSWORD`.
- Local build qua Maven trước khi push.

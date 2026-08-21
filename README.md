# Flashcard Learning App

Ứng dụng web học từ vựng theo phong cách thư viện cá nhân: người dùng tự tạo thư mục, bộ flashcard, học bằng thẻ lật, câu hỏi trắc nghiệm, bài test, game lật thẻ và bảng vẽ nhận dạng chữ viết.

## Tech Stack

- Backend: Java 21, Spring Boot 3.3.5, Spring Web, Spring Data JPA, Spring Security, Bean Validation.
- Database: PostgreSQL/Neon khi deploy, H2 file cho local nhanh.
- Frontend: Thymeleaf, HTML, CSS, Bootstrap, JavaScript thuần.
- External APIs: Azure Translator cho gợi ý nghĩa/phiên âm, Azure AI Vision cho nhận dạng chữ viết tay.
- Deploy: Docker trên Render.

## Tính Năng Chính

- Đăng ký, đăng nhập và quản lý phiên bằng Spring Security.
- Phân quyền `ROLE_USER`/`ROLE_ADMIN`, admin dashboard và khóa/mở tài khoản.
- CSRF cho form/API ghi dữ liệu, security headers, cookie session HttpOnly/SameSite/Secure khi production.
- Rate limit nhẹ cho API dịch, OCR và lưu điểm để bảo vệ quota Azure.
- Tạo, sửa, xóa thư mục học.
- Tạo, sửa, xóa bộ flashcard gồm từ vựng, phiên âm, nghĩa và ví dụ.
- Gợi ý nghĩa bằng Azure Translator khi nhập từ vựng.
- Gợi ý phiên âm Latin cho một số ngôn ngữ Azure hỗ trợ transliteration.
- Ôn tập bằng flashcard với phím tắt: mũi tên trái/phải và phím cách.
- Learn mode có trắc nghiệm, đổi chiều từ/nghĩa và bonus round cho câu sai hoặc chưa biết.
- Test mode có thời gian, số lượng câu và chiều ôn tập.
- Game lật thẻ có điểm, combo và sound effect.
- Chế độ viết đáp án bằng bảng vẽ canvas, nhận dạng chữ viết bằng Azure AI Vision OCR.

## Cấu Trúc Chính

- `src/main/java/com/flashcardapp/controller`: controller cho auth, thư viện, folder, flashcard set, practice API.
- `src/main/java/com/flashcardapp/service`: nghiệp vụ người dùng, thư viện, điểm game, dịch và OCR.
- `src/main/java/com/flashcardapp/helper/security`: helper lấy user hiện tại và filter giới hạn API.
- `src/main/java/com/flashcardapp/repository`: Spring Data JPA repository.
- `src/main/java/com/flashcardapp/entity`: entity JPA.
- `src/main/java/com/flashcardapp/dto`: form object và response object.
- `src/main/resources/templates`: giao diện Thymeleaf.
- `src/main/resources/static`: CSS và JavaScript.
- `src/main/resources/db/migration`: schema tham khảo cho PostgreSQL/Flyway khi muốn khóa schema.
- `docs`: tài liệu kiến trúc, luồng MVC, build/test và deploy.

## Chạy Local

Không cấu hình gì thêm thì app dùng H2 file local:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd spring-boot:run
```

Hoặc dùng Gradle wrapper:

```powershell
.\gradlew.bat bootRun
```

Ứng dụng chạy ở:

```text
http://localhost:8000
```

## Cấu Hình Neon

Khi muốn dùng PostgreSQL/Neon, thêm vào `.env`:

```properties
DATABASE_URL=jdbc:postgresql://your-neon-host.neon.tech/your-db?sslmode=require
DATABASE_USERNAME=your-neon-user
DATABASE_PASSWORD=your-neon-password
```

## Cấu Hình Azure

Translator dùng cho gợi ý nghĩa:

```properties
TRANSLATION_PROVIDER=azure
TRANSLATION_DEFAULT_TARGET=vi
AZURE_TRANSLATOR_KEY=your-azure-translator-key
AZURE_TRANSLATOR_REGION=global
AZURE_TRANSLATOR_ENDPOINT=https://api.cognitive.microsofttranslator.com
```

Vision dùng cho bảng vẽ nhận dạng chữ viết:

```properties
HANDWRITING_PROVIDER=azure
AZURE_VISION_KEY=your-azure-vision-key
AZURE_VISION_ENDPOINT=https://your-vision-resource.cognitiveservices.azure.com
```

Sau khi sửa `.env`, cần tắt server và chạy lại.

## Cấu Hình Admin Và Bảo Mật

Nếu muốn tạo tài khoản admin khi app khởi động, thêm vào `.env` hoặc Render Environment:

```properties
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-strong-password
ADMIN_DISPLAY_NAME=Admin
```

Local có thể để `SESSION_COOKIE_SECURE=false`. Khi deploy HTTPS trên Render, profile `prod` mặc định dùng secure cookie:

```properties
SESSION_TIMEOUT=45m
SESSION_COOKIE_SECURE=true
APP_RATE_LIMIT_ENABLED=true
APP_RATE_LIMIT_API_CAPACITY=120
APP_RATE_LIMIT_WINDOW_SECONDS=60
```

## Deploy Render

Project đã có `Dockerfile` và `render.yaml`. Trên Render, tạo Web Service runtime Docker và khai báo các biến môi trường trong `.env.example`.

Render free plan có thể sleep sau một thời gian không dùng. Đây là hành vi bình thường của gói miễn phí.


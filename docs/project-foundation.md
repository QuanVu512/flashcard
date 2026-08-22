# Nền Móng Dự Án Flashcard

## Hướng Sản Phẩm

Flashcard Learning App là web học từ vựng cá nhân, tập trung vào một workflow rõ: tạo bộ từ, chia thư mục theo chương, học bằng nhiều chế độ và theo dõi điểm qua mini game. Phạm vi này vừa đủ cho một project thực tập vì có frontend, backend, database, đăng nhập, tích hợp API ngoài và deploy thật.

## Người Dùng Mục Tiêu

- Học sinh, sinh viên tự tạo bộ từ vựng theo môn học hoặc giáo trình.
- Người học ngoại ngữ cần học từ vựng tiếng Anh, Trung, Nhật, Hàn hoặc các ngôn ngữ khác.
- Người muốn học nhanh bằng flashcard, trắc nghiệm, test và game nhẹ.

## Module Backend

- `auth`: đăng ký, OTP email, Google OpenID Connect, thiết bị tin cậy và quản lý phiên.
- `security`: phân quyền role, access JWT ngắn hạn, refresh token xoay vòng, security headers, rate limit và xử lý 401/403.
- `admin`: dashboard quản trị user, thống kê hệ thống và khóa/mở tài khoản.
- `library`: thư viện cá nhân, tìm kiếm bộ flashcard.
- `folder`: chia chương/bài học bằng thư mục.
- `flashcard-set`: tạo, sửa, xóa bộ flashcard và danh sách thẻ.
- `practice`: sinh câu hỏi Learn/Test từ dữ liệu flashcard.
- `translation`: gợi ý nghĩa và phiên âm qua Azure Translator.
- `handwriting`: nhận dạng chữ viết tay từ canvas qua Azure AI Vision.
- `game`: lưu điểm từ game lật thẻ.

## Frontend

- Static SPA render giao diện và gọi REST API bằng `fetch`.
- Bootstrap dùng cho layout cơ bản.
- CSS riêng trong `src/main/resources/static/css/app.css` để tạo giao diện giống thư viện học tập.
- `js/core` chứa API client, state, navigation và utility dùng chung.
- `js/app` chứa router và điều phối sự kiện toàn cục.
- `js/features` tách riêng auth, library, admin, study, practice, handwriting và game.
- `templates/auth`, `templates/admin`, `templates/error` và `templates/fragments` phân loại HTML theo trách nhiệm; các file này được phục vụ tĩnh, không dùng Thymeleaf.
- Access JWT, refresh token và token thiết bị nằm trong cookie `HttpOnly`; frontend không đọc hoặc lưu credential.

## Database

Database chính khi deploy là PostgreSQL/Neon. Local có thể dùng H2 file để test nhanh hoặc trỏ thẳng về Neon qua `.env`.

Bảng chính:

- `users`: tài khoản đăng nhập.
- `auth_identities`: danh tính local/Google liên kết với tài khoản.
- `auth_refresh_sessions`: phiên 3 ngày hoặc 30 ngày với token được hash.
- `auth_otp_challenges`: OTP dùng một lần và thời hạn xác minh.
- `auth_trusted_devices`: thiết bị được bỏ qua OTP trong 30 ngày.
- `clients`: hồ sơ người dùng và điểm tích lũy.
- `folders`: thư mục học của từng user.
- `flashcard_sets`: bộ flashcard.
- `flashcards`: từng thẻ từ vựng trong bộ.

## Lý Do Thiết Kế

Project không gộp tất cả flashcard vào một bảng lớn. User, client, folder, set và card được tách riêng để giữ quan hệ rõ ràng, dễ mở rộng thêm chia sẻ bộ thẻ, thống kê tiến độ hoặc admin sau này.

Ứng dụng cũng không gọi Azure trực tiếp từ frontend. Frontend gửi request về Spring Boot, backend mới gọi Azure để tránh lộ API key.

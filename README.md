# Flashcard Learning App

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-ready-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)

Flashcard Learning App là ứng dụng web tạo và học bộ từ vựng cá nhân. Người dùng có thể sắp xếp flashcard theo thư mục, học bằng nhiều chế độ, làm bài kiểm tra, chơi game ghép thẻ và dùng nhận dạng chữ viết tay.

Frontend là static SPA viết bằng HTML, CSS, Bootstrap và JavaScript thuần. Backend Spring Boot chỉ cung cấp REST API và không render dữ liệu qua Thymeleaf, nhờ đó hai phần có hợp đồng rõ ràng và có thể được triển khai độc lập khi hệ thống mở rộng.

## Tính năng

- Đăng ký, đăng nhập và đăng xuất an toàn.
- Thư viện cá nhân với tìm kiếm và thư mục.
- Tạo, chỉnh sửa và xoá bộ flashcard.
- Học bằng thẻ lật, câu hỏi lựa chọn hoặc nhập đáp án.
- Bài kiểm tra tuỳ chỉnh số câu, thời gian và chiều ôn tập.
- Game ghép cặp có chấm điểm.
- Gợi ý nghĩa và phiên âm qua Azure Translator.
- Nhận dạng đáp án viết tay qua Azure AI Vision.
- Dashboard quản trị, thống kê và khoá/mở tài khoản.
- Phân quyền `ROLE_USER` và `ROLE_ADMIN`.

## Kiến trúc

```mermaid
flowchart LR
    UI[HTML / CSS / Bootstrap / JavaScript] -->|JSON over REST| API[Spring Boot Controllers]
    API --> SERVICE[Application Services]
    SERVICE --> REPO[Spring Data JPA]
    REPO --> DB[(H2 / PostgreSQL)]
    SERVICE --> AZURE[Azure Translator / Vision]
```

Controller xử lý HTTP và DTO, service giữ nghiệp vụ và transaction, repository phụ trách truy cập dữ liệu, còn entity đại diện cho mô hình persistence. Frontend được chia theo `core`, `app` và từng `features`, tránh dồn routing, state và nghiệp vụ giao diện vào một file lớn.

### Xác thực và bảo mật

- Spring Security hoạt động stateless với OAuth2 Resource Server và JWT HS256.
- Backend gửi JWT bằng header `Set-Cookie`; cookie có `HttpOnly`, `SameSite` và `Secure` trong production.
- Frontend không đọc JWT và không lưu token trong `localStorage` hoặc `sessionStorage`.
- Các request thay đổi dữ liệu dùng CSRF token qua header `X-XSRF-TOKEN`.
- API client không chạy trong trình duyệt vẫn có thể dùng `Authorization: Bearer <token>`.
- Quyền sở hữu dữ liệu được kiểm tra ở service; API quản trị yêu cầu `ROLE_ADMIN`.
- CSP, chống nhúng iframe, Referrer Policy, Permissions Policy và rate limit cho auth/API tốn quota được cấu hình sẵn.

Xem chi tiết tại [docs/security.md](docs/security.md) và [docs/mvc-flow.md](docs/mvc-flow.md).

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Frontend | HTML5, CSS3, Bootstrap 5.3, JavaScript ES Modules |
| Backend | Java 21, Spring Boot 3.3.5, Spring Web, Spring Security |
| Persistence | Spring Data JPA, Hibernate |
| Database | H2 cho local, PostgreSQL/Neon cho production |
| Security | OAuth2 Resource Server, JWT, BCrypt, CSRF cookie |
| Tích hợp | Azure Translator, Azure AI Vision |
| Build | Gradle Wrapper hoặc Maven |
| Deploy | Docker, Render |

## Bắt đầu nhanh

### Yêu cầu

- JDK 21
- Git
- Docker là tuỳ chọn nếu muốn chạy bằng container

### Cài đặt

```bash
git clone https://github.com/QuanVu512/flashcard.git
cd flashcard
```

Ứng dụng mặc định dùng H2 file nên có thể chạy ngay mà không cần cài database ngoài.

Trên Windows:

```powershell
.\gradlew.bat bootRun
```

Trên macOS hoặc Linux:

```bash
./gradlew bootRun
```

Mở [http://localhost:8000](http://localhost:8000) và tạo tài khoản đầu tiên.

Nếu dùng Maven đã cài trên máy:

```bash
mvn spring-boot:run
```

## Cấu hình

Ứng dụng đọc biến môi trường và file `.env` ở thư mục gốc. Sao chép `.env.example` thành `.env`, sau đó chỉ điền các dịch vụ cần dùng.

| Biến | Bắt buộc | Mô tả |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Production | Dùng `dev` cho local hoặc `prod` khi deploy |
| `DATABASE_URL` | Production | JDBC URL của PostgreSQL/Neon |
| `DATABASE_USERNAME` | Production | Tài khoản database |
| `DATABASE_PASSWORD` | Production | Mật khẩu database |
| `JWT_SECRET` hoặc `JWT_BASE64_SECRET` | Production | Secret ngẫu nhiên tối thiểu 32 byte |
| `AUTH_COOKIE_SECURE` | Không | Mặc định `false` ở dev và `true` ở prod |
| `AUTH_COOKIE_SAME_SITE` | Không | `Lax`, `Strict` hoặc `None` |
| `ADMIN_EMAIL` | Không | Email tài khoản được tạo/nâng quyền admin khi khởi động |
| `ADMIN_PASSWORD` | Không | Mật khẩu bootstrap admin |
| `AZURE_TRANSLATOR_KEY` | Không | Bật gợi ý dịch khi provider là `azure` |
| `AZURE_VISION_KEY` | Không | Bật nhận dạng chữ viết khi provider là `azure` |

Không commit `.env`, database local hoặc secret thật. Profile `prod` yêu cầu `JWT_SECRET`/`JWT_BASE64_SECRET` được cấu hình và không dùng secret phát triển mặc định.

### PostgreSQL hoặc Neon

```properties
DATABASE_URL=jdbc:postgresql://host/database?sslmode=require
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-password
```

### Azure Translator

```properties
TRANSLATION_PROVIDER=azure
TRANSLATION_DEFAULT_TARGET=vi
AZURE_TRANSLATOR_KEY=your-key
AZURE_TRANSLATOR_REGION=global
AZURE_TRANSLATOR_ENDPOINT=https://api.cognitive.microsofttranslator.com
```

### Azure AI Vision

```properties
HANDWRITING_PROVIDER=azure
AZURE_VISION_KEY=your-key
AZURE_VISION_ENDPOINT=https://your-resource.cognitiveservices.azure.com
```

### Frontend ở domain riêng

Khi frontend và backend khác origin, cấu hình chính xác origin của frontend và cho phép cookie:

```properties
APP_CORS_ALLOWED_ORIGINS=https://app.example.com
AUTH_COOKIE_SECURE=true
AUTH_COOKIE_SAME_SITE=None
```

Không dùng wildcard CORS cùng credential.

Trong bản frontend được deploy riêng, đặt URL backend trong `index.html`:

```html
<meta name="flashcard-api-base-url" content="https://api.example.com">
```

## REST API chính

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Tạo tài khoản và phát cookie xác thực |
| `POST` | `/api/auth/login` | Đăng nhập và phát cookie xác thực |
| `GET` | `/api/auth/me` | Lấy hồ sơ hiện tại |
| `POST` | `/api/auth/logout` | Xoá cookie xác thực |
| `GET` | `/api/library` | Lấy thư viện của người dùng |
| `GET/POST` | `/api/folders` | Đọc hoặc tạo thư mục |
| `POST` | `/api/sets` | Tạo bộ thẻ |
| `GET/PUT/DELETE` | `/api/sets/{id}` | Đọc, cập nhật hoặc xoá bộ thẻ |
| `GET` | `/api/sets/{id}/learn` | Tạo phiên Learn |
| `GET` | `/api/sets/{id}/test` | Tạo phiên Test |
| `GET/PATCH` | `/api/admin/**` | API quản trị |

Response lỗi API dùng JSON thống nhất với `message`, `status`, `details` và `timestamp`.

## Cấu trúc dự án

```text
src/main/java/com/flashcardapp/
├── config/          # Security, JWT, CORS và configuration properties
├── controller/      # REST endpoints và SPA fallback
├── dto/             # Request/response contracts
├── entity/          # JPA entities
├── repository/      # Data access
├── service/         # Business logic và transaction boundaries
└── helper/          # Security, errors, logging và response utilities

src/main/resources/
├── static/
│   ├── js/
│   │   ├── app/      # Router và global event orchestration
│   │   ├── core/     # API client, state, navigation và utilities
│   │   ├── features/ # Auth, library, admin, study, practice và game
│   │   └── main.js   # Composition root của frontend
│   └── css/          # Stylesheet của ứng dụng
└── templates/        # HTML fragments được phục vụ tĩnh qua /views/**
    ├── admin/        # Giao diện quản trị
    ├── auth/         # Đăng nhập và đăng ký
    ├── error/        # Giao diện lỗi phía SPA
    └── fragments/    # Shell và component tái sử dụng
```

Thư mục `templates` chỉ dùng để phân loại HTML fragment cho frontend. Project không cài Thymeleaf và backend không truyền `Model` vào các file này.

## Triển khai

Repository có sẵn `Dockerfile` multi-stage và `render.yaml`. Khi triển khai production:

1. Cấu hình PostgreSQL/Neon và secret JWT.
2. Đặt `SPRING_PROFILES_ACTIVE=prod`.
3. Chỉ thêm Azure keys nếu bật các tính năng tương ứng.
4. Không đưa secret vào image hoặc repository.

Hướng dẫn chi tiết: [docs/deploy-render-docker.md](docs/deploy-render-docker.md).

## Tài liệu

- [Nền tảng và phạm vi sản phẩm](docs/project-foundation.md)
- [Luồng SPA và REST API](docs/mvc-flow.md)
- [Mô hình bảo mật](docs/security.md)
- [Quy trình build và kiểm thử](docs/build-test-workflow.md)
- [Triển khai Docker trên Render](docs/deploy-render-docker.md)

## Đóng góp

Issue và pull request nên mô tả rõ hành vi hiện tại, hành vi mong muốn và phạm vi thay đổi. Giữ controller mỏng, đặt nghiệp vụ trong service, dùng DTO ở biên API và không để frontend phụ thuộc trực tiếp vào entity backend.

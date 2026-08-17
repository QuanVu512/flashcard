# Build And Test Workflow

Mục tiêu workflow là kiểm tra nhanh trước khi commit hoặc deploy, nhưng không làm local chậm vì phải kết nối API ngoài.

## Maven Build

Build nhanh app và bỏ qua test:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd clean package -Dmaven.test.skip=true
```

Chạy app local:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd spring-boot:run
```

## Gradle Build

Project có thêm Gradle wrapper để thuận tiện khi người phỏng vấn quen Gradle:

```powershell
.\gradlew.bat --no-daemon quickCheck
```

Chạy app bằng Gradle:

```powershell
.\gradlew.bat bootRun
```

## JavaScript Check

Kiểm tra cú pháp frontend:

```powershell
node --check src\main\resources\static\js\app.js
```

## Profile Local

Local mặc định chạy port `8000`. Có thể dùng H2 file hoặc Neon qua `.env`.

```properties
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8000
```

## Profile Production

Khi deploy, nên set:

```properties
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=...
DATABASE_PASSWORD=...
```

Các biến Azure chỉ cần khai báo khi bật gợi ý dịch hoặc nhận dạng chữ viết.

## Ghi Chú Test

Các test nên ưu tiên:

- Auth pages render.
- User tạo bộ flashcard.
- Learn/Test render đúng dữ liệu.
- API score không nhận điểm âm.
- API Azure trả lỗi mềm khi thiếu key để app không chết.

# Luồng MVC Backend

Project dùng Spring MVC kết hợp Thymeleaf. Một số trang được render từ server, còn các tính năng tương tác nhỏ gọi API bằng JavaScript.

## Luồng Trang Chính

1. Browser gửi request tới Controller.
2. Controller lấy người dùng hiện tại từ `Authentication`.
3. Controller gọi Service để xử lý nghiệp vụ.
4. Service gọi Repository để đọc/ghi database.
5. Repository trả entity về Service.
6. Service chuẩn bị DTO hoặc entity đã đủ dữ liệu.
7. Controller đưa dữ liệu vào Model.
8. Thymeleaf render HTML trả về browser.

Ví dụ:

- `GET /library`: hiển thị thư viện flashcard.
- `GET /sets/{id}`: hiển thị chế độ flashcard.
- `GET /sets/{id}/learn`: hiển thị Learn mode.
- `GET /sets/{id}/test/setup`: hiển thị màn setup test.

## Luồng API Tương Tác

Một số tính năng dùng `fetch` để không phải reload trang:

1. JavaScript lấy dữ liệu người dùng nhập.
2. JavaScript gọi API nội bộ của Spring Boot.
3. Controller nhận request JSON hoặc query param.
4. Service xử lý nghiệp vụ hoặc gọi API ngoài.
5. Controller trả JSON cho JavaScript.
6. JavaScript cập nhật UI.

API hiện có:

- `GET /api/translation/suggest`: gợi ý nghĩa và phiên âm.
- `POST /api/handwriting/recognize`: nhận dạng chữ viết từ bảng vẽ.
- `POST /api/games/score`: lưu điểm game.

## Ví Dụ Gợi Ý Dịch

```javascript
fetch("/api/translation/suggest?text=hello", {
    headers: {
        Accept: "application/json"
    },
    credentials: "same-origin"
});
```

## Ví Dụ Nhận Dạng Chữ Viết

```javascript
fetch("/api/handwriting/recognize", {
    method: "POST",
    headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
    },
    credentials: "same-origin",
    body: JSON.stringify({
        imageData: canvas.toDataURL("image/png"),
        language: "zh"
    })
});
```

## Vai Trò Các Lớp

- Controller: nhận request, chọn view/API response.
- Service: xử lý nghiệp vụ như tạo bộ thẻ, sinh đáp án sai, tính điểm, gọi Azure.
- Repository: truy vấn database bằng Spring Data JPA.
- DTO: dữ liệu trao đổi giữa form/API và backend.
- Entity: ánh xạ bảng database.

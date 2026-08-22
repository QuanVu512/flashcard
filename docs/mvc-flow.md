# SPA and REST API Flow

Ứng dụng dùng Spring MVC cho REST API và một static SPA. Backend không render Thymeleaf hoặc truyền `Model` vào HTML. JavaScript/CSS nằm trong `src/main/resources/static`; HTML fragment được phân loại trong `src/main/resources/templates` và phục vụ như tài nguyên tĩnh qua `/views/**`.

## Luồng request

1. JavaScript gọi endpoint bằng `fetch` với `credentials: "include"`.
2. Trình duyệt tự gắn cookie JWT; JavaScript không thể đọc cookie `HttpOnly`.
3. Với request ghi dữ liệu, API client thêm CSRF token vào `X-XSRF-TOKEN`.
4. OAuth2 Resource Server xác thực JWT và tạo `Authentication`.
5. Controller bind request DTO, query parameter hoặc path variable.
6. Controller gọi service để xử lý nghiệp vụ.
7. Service thực thi transaction và gọi repository.
8. Controller trả response DTO; Jackson serialize thành JSON.
9. SPA render dữ liệu nhận được vào HTML.

```javascript
fetch("/api/library", {
    headers: {Accept: "application/json"},
    credentials: "include"
});
```

## Ranh giới các lớp

- Controller: giao thức HTTP, validation đầu vào và response DTO.
- Service: nghiệp vụ, transaction, quyền sở hữu dữ liệu và tích hợp Azure.
- Repository: truy vấn database qua Spring Data JPA.
- DTO: hợp đồng ổn định giữa frontend và backend.
- Entity: mô hình persistence, không dùng làm view model cho SPA.
- `js/core`: API client, state, navigation, template loader và utility dùng chung.
- `js/app`: router và điều phối sự kiện toàn cục.
- `js/features`: module giao diện theo từng nghiệp vụ, không phụ thuộc trực tiếp vào entity backend.
- `templates/auth`, `templates/admin`, `templates/error`: view theo từng nhóm màn hình.
- `templates/fragments`: shell và component HTML được nhiều feature tái sử dụng.

## Endpoint tiêu biểu

- `GET /api/library`: hồ sơ, thư mục và danh sách flashcard set.
- `POST /api/sets`: tạo bộ flashcard từ JSON.
- `PUT /api/sets/{id}`: cập nhật bộ flashcard.
- `GET /api/sets/{id}/learn`: tạo dữ liệu phiên Learn.
- `GET /api/sets/{id}/test`: tạo dữ liệu phiên Test.
- `POST /api/handwriting/recognize`: gửi ảnh canvas đến backend để gọi Azure Vision.

`SpaController` chỉ forward các route giao diện về `index.html`; `FrontendResourceConfig` chỉ ánh xạ `/views/**` tới các HTML fragment. Hai lớp này không lấy entity, không xây `Model` và không chứa nghiệp vụ.

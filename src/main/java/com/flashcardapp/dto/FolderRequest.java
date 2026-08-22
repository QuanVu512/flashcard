package com.flashcardapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderRequest(
        @NotBlank(message = "Vui lòng nhập tên thư mục")
        @Size(max = 140, message = "Tên thư mục tối đa 140 ký tự")
        String name,

        @Size(max = 280, message = "Mô tả tối đa 280 ký tự")
        String description
) {
}

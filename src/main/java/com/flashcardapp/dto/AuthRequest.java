package com.flashcardapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank(message = "Vui lòng nhập email")
        @Email(message = "Email chưa đúng định dạng")
        String email,

        @NotBlank(message = "Vui lòng nhập mật khẩu")
        @Size(max = 128, message = "Mật khẩu tối đa 128 ký tự")
        String password
) {
}

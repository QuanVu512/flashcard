package com.flashcardapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLinkRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu tài khoản hiện tại")
        @Size(max = 128, message = "Mật khẩu tối đa 128 ký tự")
        String password
) {
}

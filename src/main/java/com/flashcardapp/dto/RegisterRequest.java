package com.flashcardapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập tên hiển thị")
    @Size(max = 120, message = "Tên hiển thị tối đa 120 ký tự")
    private String displayName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email chưa đúng định dạng")
    private String email;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,32}$",
            message = "Mật khẩu phải dài từ 6 đến 32 ký tự và có ít nhất 1 chữ cái, 1 chữ số"
    )
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    @Size(max = 128, message = "Mật khẩu xác nhận tối đa 128 ký tự")
    private String confirmPassword;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}

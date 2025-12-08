package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistrationRequest {
    private String name;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống.")
    private String confirmPassword;
}
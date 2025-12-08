package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MerchantRegisterRequest {
    // Thông tin bắt buộc (*)
    @NotBlank(message = "Email không được để trống.")
    @Email(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "Email không hợp lệ.")
    private String email;

    @NotBlank(message = "Password không được để trống.")
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Xác nhận Password không được để trống.")
    private String confirmPassword;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^(0|\\+84)\\d{9,10}$", message = "Số điện thoại không hợp lệ.")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống.")
    private String address; // Địa chỉ cho cả User và Merchant

    // Thông tin Merchant
    // Tên nhà hàng có thể không bắt buộc theo yêu cầu (Nếu nhập tên nhà hàng, thì mới lưu)
    private String restaurantName;

    // Thêm các validation khác nếu cần:
    @AssertTrue(message = "Mật khẩu xác nhận không khớp.")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }
}

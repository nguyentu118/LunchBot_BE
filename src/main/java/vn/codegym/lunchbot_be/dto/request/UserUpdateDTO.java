package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.Gender;

import java.time.LocalDate;

@Data
public class UserUpdateDTO {
    // Tên (*)
    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 100, message = "Tên phải có từ 2 đến 100 ký tự")
    private String fullName;

    // Ngày sinh (Không bắt buộc)
    private LocalDate dateOfBirth;

    // Giới tính (Không bắt buộc)
    private Gender gender;

    // Địa chỉ giao hàng (*)
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(min = 5, max = 255, message = "Địa chỉ giao hàng phải có từ 5 đến 255 ký tự")
    private String shippingAddress;
}

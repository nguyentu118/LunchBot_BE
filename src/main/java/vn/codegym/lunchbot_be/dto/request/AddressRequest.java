package vn.codegym.lunchbot_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String contactName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại phải có 10-11 chữ số")
    private String phone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    // ✅ GHN Province ID
    private Integer provinceId;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    // ✅ GHN District ID
    private Integer districtId;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    // ✅ GHN Ward Code
    private String wardCode;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    private String street;

    private String building; // Tòa nhà, số tầng (optional)

    private Boolean isDefault; // Đặt làm địa chỉ mặc định
}
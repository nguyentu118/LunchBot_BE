package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingPartnerRequest {
    @NotBlank(message = "Tên đối tác không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private Boolean isDefault;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải có 10 chữ số")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotNull(message = "Chiết khấu không được để trống")
    @DecimalMin(value = "0.0", message = "Chiết khấu không được âm")
    @DecimalMax(value = "100.0", message = "Chiết khấu không được vượt quá 100")
    private BigDecimal commissionRate;

    @Size(max = 500, message = "Lý do khóa không được vượt quá 500 kí tự")
    private String lockReason;
}

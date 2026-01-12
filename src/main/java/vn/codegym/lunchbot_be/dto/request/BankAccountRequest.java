package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountRequest {

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^[0-9]{9,19}$", message = "Số tài khoản phải từ 9-19 chữ số")
    private String bankAccountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Pattern(regexp = "^[A-Z\\s]+$", message = "Tên chủ tài khoản phải viết hoa không dấu (VD: NGUYEN VAN A)")
    private String bankAccountHolder;
}
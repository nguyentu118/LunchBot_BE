package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBankInfoRequest {

    @NotBlank(message = "Số tài khoản không được để trống")
    @Pattern(regexp = "^[0-9]{8,20}$", message = "Số tài khoản phải từ 8-20 chữ số")
    private String bankAccountNumber;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    @Size(max = 100, message = "Tên ngân hàng tối đa 100 ký tự")
    private String bankName;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    @Size(max = 100, message = "Tên chủ tài khoản tối đa 100 ký tự")
    @Pattern(regexp = "^[A-Z\\s]+$", message = "Tên chủ tài khoản phải viết HOA, không dấu")
    private String bankAccountName; // VD: NGUYEN VAN A

    @Size(max = 100, message = "Tên chi nhánh tối đa 100 ký tự")
    private String bankBranch; // Optional
}
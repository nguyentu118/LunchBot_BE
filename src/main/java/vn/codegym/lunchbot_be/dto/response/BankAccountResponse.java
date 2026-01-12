package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {
    private Long merchantId;
    private String restaurantName;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private Boolean hasLinkedBank; // Đã liên kết ngân hàng chưa
}
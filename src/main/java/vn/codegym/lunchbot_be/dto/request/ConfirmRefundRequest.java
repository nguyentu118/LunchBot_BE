package vn.codegym.lunchbot_be.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmRefundRequest {
    private String refundTransactionRef; // Mã giao dịch hoàn tiền
    private String notes; // Ghi chú
}
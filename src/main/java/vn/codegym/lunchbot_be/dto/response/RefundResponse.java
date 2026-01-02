package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private String customerEmail;
    private String customerName;
    private BigDecimal refundAmount;
    private String customerBankAccount;
    private String customerBankName;
    private String customerAccountName;
    private RefundStatus refundStatus;
    private String refundReason;
    private String transactionRef;
    private String refundTransactionRef;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String processedBy;
    private String notes;
}
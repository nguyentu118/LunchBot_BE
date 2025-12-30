package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelledOrderDTO {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private String cancelledBy; // "CUSTOMER" hoặc "MERCHANT"
}
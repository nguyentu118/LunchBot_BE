package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRevenueDetailDTO {
    private Long orderId;
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime completedAt;
    private BigDecimal itemsTotal;
    private BigDecimal discountAmount;
    private BigDecimal revenue; // itemsTotal - discountAmount
}
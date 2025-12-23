package vn.codegym.lunchbot_be.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponStatisticsResponse {
    private Long couponId;
    private String couponCode;
    // --- Phần doanh thu ở trên ---
    private long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalDiscountGiven; // Tổng số tiền đã giảm cho khách
    // --- Danh sách đơn ở dưới ---
    private List<OrderResponse> orders;
}

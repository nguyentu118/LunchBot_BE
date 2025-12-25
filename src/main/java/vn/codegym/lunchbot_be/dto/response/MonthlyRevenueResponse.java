package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenueResponse {
    private Long merchantId;
    private YearMonth yearMonth;
    private Integer totalOrders;

    // Doanh thu gộp (chưa trừ phí)
    private BigDecimal totalGrossRevenue;

    // Mức chiết khấu áp dụng (%)
    private BigDecimal platformCommissionRate;

    // Tổng phí chiết khấu sàn
    private BigDecimal totalPlatformFee;

    // Doanh thu ròng (đã trừ phí)
    private BigDecimal netRevenue;

    // Chi tiết từng đơn hàng
    private List<OrderRevenueDetailDTO> orderDetails;
}
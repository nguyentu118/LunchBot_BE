package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportDTO {
    // Thông tin tổng quát
    private Long merchantId;
    private String merchantName;
    private String period; // "01/2025" hoặc "Tháng 1 năm 2025"
    private LocalDateTime exportedAt;

    // Doanh thu tổng
    private BigDecimal totalGrossRevenue;

    // Số lượng đơn
    private Integer totalOrders;
    private Integer completedOrders;
    private List<CompletedOrderDTO> completedOrderDetails;

    private Integer cancelledOrders;
    private List<CancelledOrderDTO> cancelledOrderDetails;

    // Giá trị trung bình đơn
    private BigDecimal averageOrderValue;

    // Chiết khấu sàn
    private BigDecimal platformCommissionRate; // %
    private BigDecimal totalPlatformFee;

    // Doanh thu ròng
    private BigDecimal netRevenue;

    // So sánh kì trước
    private BigDecimal previousMonthRevenue;
    private BigDecimal revenueChange; // Số tiền thay đổi
    private BigDecimal revenueChangePercent; // % thay đổi
    private String revenueChangeStatus; // "UP", "DOWN", "EQUAL"
}
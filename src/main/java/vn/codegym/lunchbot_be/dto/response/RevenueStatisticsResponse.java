package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class RevenueStatisticsResponse {
    // Thông tin tổng quan (Hiển thị bên trên)
    private BigDecimal totalRevenue; // Tổng tiền
    private Long totalOrders;

    private String timeRange;           // "WEEK", "MONTH", "QUARTER", "YEAR"
    private LocalDateTime startDate;    // Ngày bắt đầu
    private LocalDateTime endDate;      // Ngày kết thúc

    // Danh sách chi tiết (Hiển thị bên dưới)
    private Page<OrderResponse> orders;
}

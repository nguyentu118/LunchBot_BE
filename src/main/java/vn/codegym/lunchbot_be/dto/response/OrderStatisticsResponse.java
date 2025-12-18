package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {

    // Số lượng đơn theo từng trạng thái
    private Long pendingCount;      // Chờ xác nhận
    private Long confirmedCount;    // Đã xác nhận
    private Long processingCount;   // Đang chế biến
    private Long readyCount;        // Đã xong món
    private Long deliveringCount;   // Đang giao
    private Long completedCount;    // Hoàn thành
    private Long cancelledCount;    // Đã hủy

    // Tổng số đơn
    private Long totalOrders;

    // Thống kê bổ sung (optional)
    private Long todayOrders;       // Đơn hôm nay
    private Long activeOrders;      // Đơn đang xử lý (PENDING + PROCESSING + READY + DELIVERING)

    /**
     * Helper method để tính tổng
     */
    public void calculateTotal() {
        this.totalOrders = pendingCount + confirmedCount + processingCount +
                readyCount + deliveringCount + completedCount + cancelledCount;

        this.activeOrders = pendingCount + processingCount + readyCount + deliveringCount;
    }
}
package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationSummaryResponse {

    // Thống kê tổng quan cho Merchant
    private Long totalRequests;        // Tổng số request
    private Long pendingRequests;      // Số request đang chờ
    private Long approvedRequests;     // Số request đã duyệt
    private Long rejectedRequests;     // Số request bị từ chối

    // Request gần nhất
    private ReconciliationRequestResponse latestRequest;
}

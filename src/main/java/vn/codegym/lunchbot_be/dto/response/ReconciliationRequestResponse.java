package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRequestResponse {

    private Long id;

    // Merchant info
    private Long merchantId;
    private String merchantName;
    private String merchantEmail;
    private String merchantPhone;

    // Reconciliation info
    private String yearMonth;

    // Financial data
    private Integer totalOrders;
    private BigDecimal totalGrossRevenue;
    private BigDecimal platformCommissionRate;
    private BigDecimal totalPlatformFee;
    private BigDecimal netRevenue;

    // Status
    private ReconciliationStatus status;
    private String statusDisplay; // "Đang chờ", "Đã duyệt", "Đã từ chối"

    // Admin review
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String rejectionReason;

    // Notes
    private String merchantNotes;
    private String adminNotes;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper methods
    public String getStatusDisplay() {
        if (status == null) return "";
        switch (status) {
            case PENDING: return "Đang chờ xử lý";
            case REPORTED: return "Đang khiếu nại"; // <--- Thêm dòng này
            case APPROVED: return "Đã duyệt";
            case REJECTED: return "Đã từ chối";
            default: return status.name();
        }
    }
}
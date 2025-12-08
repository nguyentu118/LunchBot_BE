package vn.codegym.lunchbot_be.dto.response;

import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminMerchantListResponse {
    private Long id;
    private String restaurantName;
    private String ownerName;
    private String email;
    private String phone;
    private MerchantStatus status;
    private Boolean isLocked;
    private Boolean isApproved;
    private BigDecimal revenueTotal;
    private BigDecimal currentBalance;
    private LocalDateTime registrationDate;
    private Integer dishCount;
    private Integer orderCount;

    // Cho hiển thị trong bảng
    public String getStatusBadge() {
        if (Boolean.TRUE.equals(isLocked)) {
            return "LOCKED";
        }
        return status.toString();
    }

    public String getStatusColor() {
        if (Boolean.TRUE.equals(isLocked)) {
            return "error";
        }
        switch (status) {
            case APPROVED:
                return "success";
            case PENDING:
                return "warning";
            case REJECTED:
                return "error";
            default:
                return "default";
        }
    }
}


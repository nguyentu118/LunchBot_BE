package vn.codegym.lunchbot_be.dto.response;

import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.PartnerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminMerchantResponse {
    private Long id;
    private String restaurantName;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private String openTime;
    private String closeTime;
    private BigDecimal revenueTotal;
    private BigDecimal currentBalance;
    private MerchantStatus status;
    private Boolean isLocked;
    private Boolean isApproved;
    private String rejectionReason;
    private LocalDateTime registrationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime lockedAt;
    private PartnerStatus partnerStatus;

    // Statistics
    private Integer dishCount;
    private Integer orderCount;
    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private BigDecimal monthlyRevenue;

    // Danh sách món ăn (cho task 27)
    private List<DishSimpleResponse> dishes;
}

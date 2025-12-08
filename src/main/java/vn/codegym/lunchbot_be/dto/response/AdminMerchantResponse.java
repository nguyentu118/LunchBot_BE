package vn.codegym.lunchbot_be.dto.response;

import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class AdminMerchantResponse {
    private Long id;
    private String restaurantName;
    private String ownerName;
    private String email;
    private String phone;
    private String address;
    private LocalTime openTime;
    private LocalTime closeTime;
    private BigDecimal revenueTotal;
    private BigDecimal currentBalance;
    private MerchantStatus status;
    private Boolean isPartner;
    private Boolean isLocked;
    private Boolean isApproved;
    private String rejectionReason;
    private LocalDateTime registrationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime lockedAt;

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

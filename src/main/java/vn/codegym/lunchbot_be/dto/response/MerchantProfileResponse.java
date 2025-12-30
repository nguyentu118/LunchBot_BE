package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.PartnerStatus;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class MerchantProfileResponse {
    private Long merchantId;
    private String restaurantName;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalTime openTime;
    private LocalTime closeTime;
    private PartnerStatus partnerStatus;
    private BigDecimal currentMonthRevenue;
    private BigDecimal currentBalance;
    private BigDecimal revenueTotal;
}

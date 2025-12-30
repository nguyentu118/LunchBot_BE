package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.Coupon;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCouponsResponse {
    private Long merchantId;
    private String restaurantName;
    private String address;
    private String avatarUrl;
    private String phone;
    private List<CouponDetailResponse> coupons;
}
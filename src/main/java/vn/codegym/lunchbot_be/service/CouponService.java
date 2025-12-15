package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.CouponRequest;
import vn.codegym.lunchbot_be.dto.response.CouponResponse;
import vn.codegym.lunchbot_be.model.Coupon;

import java.util.List;

public interface CouponService {
    CouponResponse validateAndCalculate(CouponRequest request);

    List<Coupon> getAllCouponsByMerchant(Long merchantId);

    List<Coupon> getActiveCouponsByMerchant(Long merchantId);
}

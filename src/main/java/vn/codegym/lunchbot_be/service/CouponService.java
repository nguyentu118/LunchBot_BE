package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.codegym.lunchbot_be.dto.request.CouponCreateRequest;
import vn.codegym.lunchbot_be.dto.request.CouponRequest;
import vn.codegym.lunchbot_be.dto.response.CouponResponse;
import vn.codegym.lunchbot_be.dto.response.PaginatedCouponsResponse;
import vn.codegym.lunchbot_be.model.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    CouponResponse validateAndCalculate(CouponRequest request);

    List<Coupon> getAllCouponsByMerchant(Long merchantId);

    List<Coupon> getActiveCouponsByMerchant(Long merchantId);

    void deleteCoupon(Long couponId);

    Coupon updateCoupon(Long couponId, CouponCreateRequest request);

    PaginatedCouponsResponse getAllCouponsGroupedByMerchant(
            boolean onlyActive,
            String keyword,
            String sortBy,
            Pageable pageable);
}

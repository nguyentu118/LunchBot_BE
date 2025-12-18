package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.CouponRequest;
import vn.codegym.lunchbot_be.dto.response.CouponResponse;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.scheduler.CouponScheduler;
import vn.codegym.lunchbot_be.service.CouponService;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    private final CouponService couponService;

    private final CouponScheduler couponScheduler;

    @PostMapping("/validate")
    public ResponseEntity<CouponResponse> validateCoupon(@RequestBody CouponRequest request) {
        CouponResponse response = couponService.validateAndCalculate(request);

        if (!response.isValid()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{merchantId}/active")
    public ResponseEntity<List<Coupon>> getActiveCouponsByMerchant(@PathVariable Long merchantId) {
        List<Coupon> coupons = couponService.getActiveCouponsByMerchant(merchantId);
        return ResponseEntity.ok(coupons);
    }
}

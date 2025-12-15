package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.CouponCreateRequest;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.dto.response.MerchantResponseDTO;
import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.impl.CouponServiceImpl;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantServiceImpl merchantService;

    private final CouponServiceImpl couponService;

    @GetMapping("/current/id")
    public ResponseEntity<?> getCurrentMerchantId(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // Lấy trực tiếp từ UserDetails thay vì Authentication
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            return ResponseEntity.ok(Map.of("merchantId", merchantId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống khi lấy Merchant ID.");
        }
    }

    @GetMapping("/profile")
    // Authentication object được inject tự động bởi Spring Security
    public ResponseEntity<MerchantResponseDTO> getMerchantProfile(Authentication authentication) {
        // 1. Lấy email của Merchant đang đăng nhập từ Security Context
        String merchantEmail = authentication.getName();

        // 2. Gọi Service để lấy thông tin chi tiết
        MerchantResponseDTO profile = merchantService.getMerchantProfileByEmail(merchantEmail);

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<Merchant> updateMerchantInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MerchantUpdateRequest request
    ) {
        Long UserId = userDetails.getId();
        Merchant updatedMerchant = merchantService.updateMerchanntInfo(UserId, request);
        return ResponseEntity.ok(updatedMerchant);
    }


     //GET /api/merchants/popular?limit=8
    @GetMapping("/popular")
    public ResponseEntity<List<PopularMerchantDto>> getPopularMerchants(
            @RequestParam(defaultValue = "8") int limit
    ) {
        try {
            // Giới hạn max 50 để tránh query quá nhiều
            if (limit > 50) {
                limit = 50;
            }

            List<PopularMerchantDto> merchants = merchantService.getPopularMerchants(limit);

            if (merchants.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(merchants, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("❌ Error fetching popular merchants: " + e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/create-coupon")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> createCoupon(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CouponCreateRequest request
    ) {
        try {
            Long userId = userDetails.getId();
            Long currentMerchantId = merchantService.getMerchantIdByUserId(userId);

            Coupon newCoupon = couponService.createCoupon(currentMerchantId, request);
            return ResponseEntity.ok(newCoupon);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-coupons/active")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getMyActiveCoupons(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);
            List<Coupon> coupons = couponService.getActiveCouponsByMerchant(merchantId);
            return ResponseEntity.ok(coupons);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-coupons")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getMyCoupons(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);
            List<Coupon> coupons = couponService.getAllCouponsByMerchant(merchantId);
            return ResponseEntity.ok(coupons);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}

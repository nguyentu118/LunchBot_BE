package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.CouponCreateRequest;
import vn.codegym.lunchbot_be.dto.request.CouponRequest;
import vn.codegym.lunchbot_be.dto.response.CouponResponse;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.enums.DiscountType;
import vn.codegym.lunchbot_be.repository.CouponRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {
    private final CouponRepository couponRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public CouponResponse validateAndCalculate(CouponRequest request) {
        Coupon coupon = couponRepository.findByCodeAndMerchantId(request.getCode(), request.getMerchantId())
                .orElse(null);

        if (coupon == null) {
            return CouponResponse.builder()
                    .valid(false)
                    .message("Mã giảm giá không tồn tại hoặc không thuộc của hàng này.")
                    .discountAmount(BigDecimal.ZERO)
                    .finalTotal(request.getOrderTotal())
                    .build();
        }

        if (!coupon.getIsActive()) {
            return buildErrorResponse("Mã giảm giá đã bị khóa.", request.getOrderTotal());
        }

        LocalDate now = LocalDate.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            return buildErrorResponse("Mã giảm giá chưa đến đợt hoặc đã hết hạn.", request.getOrderTotal());
        }

        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return buildErrorResponse("Mã giảm giá đã hết lượt sử dụng.", request.getOrderTotal());
        }

        if (request.getOrderTotal().compareTo(coupon.getMinOrderValue()) < 0) {
            return buildErrorResponse(
                    "Đơn hàng phải từ " + coupon.getMinOrderValue() + "đ mới được sử dụng mã này.",
                    request.getOrderTotal()
            );
        }

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = coupon.getDiscountValue().divide(new BigDecimal(100));
            discountAmount = request.getOrderTotal().multiply(percentage);
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        if (discountAmount.compareTo(request.getOrderTotal()) > 0) {
            discountAmount = request.getOrderTotal();
        }

        BigDecimal finalTotal = request.getOrderTotal().subtract(discountAmount);

        return CouponResponse.builder()
                .valid(true)
                .message("Áp dụng mã thành công!")
                .couponCode(coupon.getCode())
                .discountAmount(discountAmount)
                .finalTotal(finalTotal)
                .build();
    }

    private CouponResponse buildErrorResponse(String message, BigDecimal total) {
        return CouponResponse.builder()
                .valid(false)
                .message(message)
                .discountAmount(BigDecimal.ZERO)
                .finalTotal(total)
                .build();
    }

    public Coupon createCoupon(Long merchantId, CouponCreateRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại"));

        Coupon coupon = Coupon.builder()
                .merchant(merchant)
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .isActive(true)
                .build();

        return couponRepository.save(coupon);
    }

    /**
     * Lấy TẤT CẢ coupon của merchant (dành cho merchant xem)
     */
    @Override
    public List<Coupon> getAllCouponsByMerchant(Long merchantId) {
        return couponRepository.findByMerchantId(merchantId);
    }

    /**
     * Lấy chỉ các coupon còn hiệu lực (dành cho người dùng xem hoặc merchant lọc)
     */
    @Override
    public List<Coupon> getActiveCouponsByMerchant(Long merchantId) {
        return couponRepository.findActiveCouponsByMerchant(merchantId, LocalDate.now());
    }
}
package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.CouponCreateRequest;
import vn.codegym.lunchbot_be.dto.request.CouponRequest;
import vn.codegym.lunchbot_be.dto.response.CouponDetailResponse;
import vn.codegym.lunchbot_be.dto.response.CouponResponse;
import vn.codegym.lunchbot_be.dto.response.MerchantCouponsResponse;
import vn.codegym.lunchbot_be.dto.response.PaginatedCouponsResponse;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.enums.DiscountType;
import vn.codegym.lunchbot_be.repository.CouponRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    public void deleteCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy coupon"));
        couponRepository.delete(coupon);
    }

    @Override
    @Transactional
    public Coupon updateCoupon(Long couponId, CouponCreateRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy coupon"));

        // ✅ Cập nhật ĐẦY ĐỦ các trường từ request
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setMinOrderValue(request.getMinOrderValue());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());

        // ✅ Save và return
        return couponRepository.save(coupon);
    }

    // Thay thế method getAllCouponsGroupedByMerchant trong CouponServiceImpl.java

    @Override
    public PaginatedCouponsResponse getAllCouponsGroupedByMerchant(
            boolean onlyActive,
            String keyword,
            String sortBy,
            Pageable pageable) {

        // 1️⃣ Lấy TẤT CẢ coupons (không phân trang ở đây)
        List<Coupon> allCoupons;

        if (keyword != null && !keyword.trim().isEmpty()) {
            allCoupons = couponRepository.searchActiveCoupons(
                    keyword.trim(),
                    LocalDate.now(),
                    Pageable.unpaged() // Không phân trang
            ).getContent();
        } else if (onlyActive) {
            allCoupons = couponRepository.findAllActiveCouponsWithPagination(
                    LocalDate.now(),
                    Pageable.unpaged() // Không phân trang
            ).getContent();
        } else {
            allCoupons = couponRepository.findAllCouponsWithPagination(
                    Pageable.unpaged() // Không phân trang
            ).getContent();
        }

        // 2️⃣ Group theo merchant
        Map<Long, List<Coupon>> couponsByMerchant = allCoupons.stream()
                .collect(Collectors.groupingBy(coupon -> coupon.getMerchant().getId()));

        // 3️⃣ Convert sang MerchantCouponsResponse
        List<MerchantCouponsResponse> allMerchantResponses = couponsByMerchant.entrySet().stream()
                .map(entry -> {
                    Long merchantId = entry.getKey();
                    List<Coupon> coupons = entry.getValue();

                    Merchant merchant = coupons.get(0).getMerchant();

                    List<CouponDetailResponse> couponDetails = coupons.stream()
                            .map(this::convertToCouponDetailResponse)
                            .collect(Collectors.toList());

                    // Sort coupons trong mỗi merchant
                    if ("discount_high".equalsIgnoreCase(sortBy)) {
                        couponDetails.sort(Comparator.comparing(
                                CouponDetailResponse::getDiscountValue,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ));
                    } else if ("discount_low".equalsIgnoreCase(sortBy)) {
                        couponDetails.sort(Comparator.comparing(
                                CouponDetailResponse::getDiscountValue,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ));
                    }

                    return MerchantCouponsResponse.builder()
                            .merchantId(merchantId)
                            .restaurantName(merchant.getRestaurantName())
                            .address(merchant.getAddress())
                            .avatarUrl(merchant.getAvatarUrl())
                            .phone(merchant.getPhone())
                            .coupons(couponDetails)
                            .build();
                })
                .collect(Collectors.toList());

        // 4️⃣ Sort merchants theo discount value
        if ("discount_high".equalsIgnoreCase(sortBy)) {
            allMerchantResponses.sort((a, b) -> {
                BigDecimal maxA = a.getCoupons().stream()
                        .map(CouponDetailResponse::getDiscountValue)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal maxB = b.getCoupons().stream()
                        .map(CouponDetailResponse::getDiscountValue)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                return maxB.compareTo(maxA); // Giảm dần
            });
        } else if ("discount_low".equalsIgnoreCase(sortBy)) {
            allMerchantResponses.sort((a, b) -> {
                BigDecimal minA = a.getCoupons().stream()
                        .map(CouponDetailResponse::getDiscountValue)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal minB = b.getCoupons().stream()
                        .map(CouponDetailResponse::getDiscountValue)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                return minA.compareTo(minB); // Tăng dần
            });
        } else {
            // Mặc định: sort theo số lượng coupons (nhiều nhất trước)
            allMerchantResponses.sort((a, b) ->
                    Integer.compare(b.getCoupons().size(), a.getCoupons().size())
            );
        }

        // 5️⃣ ✅ PHÂN TRANG MERCHANTS (không phải coupons!)
        int totalMerchants = allMerchantResponses.size();
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();

        int startIndex = pageNumber * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalMerchants);

        // Đảm bảo không vượt quá bounds
        if (startIndex >= totalMerchants) {
            startIndex = 0;
            endIndex = 0;
        }

        List<MerchantCouponsResponse> paginatedMerchants =
                (startIndex < totalMerchants)
                        ? allMerchantResponses.subList(startIndex, endIndex)
                        : List.of();

        int totalPages = (int) Math.ceil((double) totalMerchants / pageSize);

        // 6️⃣ Return paginated response
        return PaginatedCouponsResponse.builder()
                .content(paginatedMerchants)
                .currentPage(pageNumber)
                .totalPages(totalPages)
                .totalElements((long) totalMerchants) // Tổng số MERCHANTS
                .pageSize(pageSize)
                .hasNext(pageNumber < totalPages - 1)
                .hasPrevious(pageNumber > 0)
                .build();
    }

    // ✅ Helper method để convert Coupon sang CouponDetailResponse
    private CouponDetailResponse convertToCouponDetailResponse(Coupon coupon) {
        return CouponDetailResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .isActive(coupon.getIsActive())
                .remainingUsage(coupon.getUsageLimit() - coupon.getUsedCount())
                .build();
    }

}
package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    // ========== THÔNG TIN CỬA HÀNG ==========
    private Long merchantId;
    private String merchantName;
    private String merchantAddress;
    private String merchantPhone;

    // ========== DANH SÁCH MÓN ĂN ==========
    private List<CartItemDTO> items;
    private Integer totalItems; // Tổng số lượng món

    // ========== ĐỊA CHỈ GIAO HÀNG ==========
    private List<AddressResponse> addresses; // Tất cả địa chỉ của user
    private Long defaultAddressId; // ID địa chỉ mặc định

    // ========== TÍNH TOÁN GIÁ ==========
    private BigDecimal itemsTotal;      // Tổng tiền món ăn
    private BigDecimal discountAmount;  // Số tiền giảm giá
    private BigDecimal serviceFee;      // Phí dịch vụ (0đ)
    private BigDecimal shippingFee;     // Phí vận chuyển (15k hoặc 25k)
    private BigDecimal totalAmount;     // Tổng thanh toán

    // ========== MÃ GIẢM GIÁ ==========
    private String appliedCouponCode;   // Mã đã áp dụng
    private Boolean canUseCoupon;       // Có thể dùng mã không
    private List<CouponInfo> availableCoupons; // Danh sách mã có thể dùng

    // ========== META INFO ==========
    private String notes; // Ghi chú từ request trước (nếu có)

    /**
     * Inner class cho thông tin coupon
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponInfo {
        private Long id;
        private String code;
        private String description;
        private BigDecimal discountValue;
        private String discountType; // "PERCENTAGE" hoặc "FIXED_AMOUNT"
        private BigDecimal minOrderValue;
    }
}
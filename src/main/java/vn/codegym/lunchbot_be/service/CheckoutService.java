package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.response.CheckoutResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service xử lý logic tính toán cho trang thanh toán
 */
public interface CheckoutService {

    /**
     * Lấy thông tin đầy đủ cho trang checkout
     */
    CheckoutResponse getCheckoutInfo(String email, List<Long> selectedDishIds);

    /**
     * Tính toán lại giá khi áp dụng mã giảm giá
     */
    CheckoutResponse applyDiscount(String email, String couponCode, List<Long> selectedDishIds);

    /**
     * Tính phí dịch vụ (Service Fee)
     */
    BigDecimal calculateServiceFee(BigDecimal itemsTotal);

    /**
     * Tính phí vận chuyển (Shipping Fee)
     */
    BigDecimal calculateShippingFee(String province);
    /**
     * Validate giỏ hàng trước khi checkout
     */
    void validateCart(String email);
}

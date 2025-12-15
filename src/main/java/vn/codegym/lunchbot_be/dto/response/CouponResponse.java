package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CouponResponse {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
    private String couponCode;
}

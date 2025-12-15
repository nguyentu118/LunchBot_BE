package vn.codegym.lunchbot_be.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponRequest {
    private String code;
    private Long merchantId;
    private BigDecimal orderTotal;
}

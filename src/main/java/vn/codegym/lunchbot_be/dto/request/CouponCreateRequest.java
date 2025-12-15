package vn.codegym.lunchbot_be.dto.request;

import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CouponCreateRequest {
    private String code;              // Ví dụ: KHAI-TRUONG
    private DiscountType discountType; // PERCENTAGE hoặc FIXED_AMOUNT
    private BigDecimal discountValue;  // 10 (10%) hoặc 20000 (20k)
    private Integer usageLimit;        // Giới hạn số lượt dùng (vd: 100)
    private BigDecimal minOrderValue;  // Đơn tối thiểu (vd: 50k)
    private LocalDate validFrom;       // Ngày bắt đầu
    private LocalDate validTo;         // Ngày kết thúc
}

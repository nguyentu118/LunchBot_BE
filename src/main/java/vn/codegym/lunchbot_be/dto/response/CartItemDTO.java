package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Long id;
    private Long dishId;
    private String dishName;
    private String dishImage;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    private Long merchantId;
    private String merchantName;
    private String merchantAddress;
}

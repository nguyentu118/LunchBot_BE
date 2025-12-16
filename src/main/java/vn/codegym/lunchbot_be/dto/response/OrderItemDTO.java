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
public class OrderItemDTO {

    private Long id; // OrderItem ID
    private Long dishId;
    private String dishName;
    private String dishImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice; // unitPrice * quantity
}
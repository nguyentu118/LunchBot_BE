package vn.codegym.lunchbot_be.dto.request;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long dishId;
    private Integer quantity;
}

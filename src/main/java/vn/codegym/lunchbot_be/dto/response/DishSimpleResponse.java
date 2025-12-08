package vn.codegym.lunchbot_be.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DishSimpleResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Boolean isActive;
    private Integer viewCount;
    private Integer orderCount;
}

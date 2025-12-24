package vn.codegym.lunchbot_be.dto.request;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;

@Data
@Builder
public class DishSearchRequest {
    private String name;
    private String categoryName;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean isRecommended;

    private int page = 0;
    private int size = 10;
}

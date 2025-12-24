package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DishSearchResponse {
    private Long id;
    private String name;
    private String imagesUrls;
    private BigDecimal price;
    private String restaurantName;
    private Boolean isRecommended;
}

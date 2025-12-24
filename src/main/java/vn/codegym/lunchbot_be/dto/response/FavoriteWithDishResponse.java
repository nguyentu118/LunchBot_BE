package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteWithDishResponse {
    private Long id;
    private Long userId;
    private Long dishId;
    private LocalDateTime createdAt;
    private DishInfo dish;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishInfo {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private BigDecimal discountPrice;
        private Integer preparationTime;
        private String merchantName;
        private List<String> images;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DishImageInfo {
        private Long id;
        private String imageUrl;
        private Integer displayOrder;
        private Boolean isPrimary;
    }
}
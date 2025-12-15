package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Category;

@Data
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private String iconUrl;
    private int restaurantCount;

    public static CategoryDto fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        int count = 0;
        if (category.getDishes() != null) {
            count = (int) category.getDishes().stream()
                    // Giả định Dish có phương thức getMerchant()
                    .map(dish -> dish.getMerchant())
                    // Loại bỏ các món ăn không có nhà hàng liên kết
                    .filter(java.util.Objects::nonNull)
                    // Đếm các nhà hàng duy nhất
                    .distinct()
                    .count();
        }
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .iconUrl(category.getIconUrl())
                .restaurantCount(count)
                .build();
    }
}

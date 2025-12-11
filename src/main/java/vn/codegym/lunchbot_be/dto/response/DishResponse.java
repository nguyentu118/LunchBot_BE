package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class DishResponse {

    private Long id;

    // 💡 SỬ DỤNG MERCHANT DTO
    private MerchantResponseDTO merchant;

    private String name;
    private String description;
    private String imagesUrls;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal serviceFee;
    private Integer preparationTime;

    private Integer viewCount;
    private Integer orderCount;

    private Boolean isRecommended;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 💡 SỬ DỤNG CATEGORY DTO
    private List<CategoryResponse> categories;

    public static DishResponse fromEntity(Dish dish) {
        if (dish == null) {
            return null;
        }

        // Chuyển đổi Set<Category> thành List<CategoryResponse>
        List<CategoryResponse> categoryResponses = dish.getCategories().stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());

        return DishResponse.builder()
                .id(dish.getId())
                .merchant(MerchantResponseDTO.fromEntity(dish.getMerchant())) // Convert Merchant
                .name(dish.getName())
                .description(dish.getDescription())
                .imagesUrls(dish.getImagesUrls())
                .price(dish.getPrice())
                .discountPrice(dish.getDiscountPrice())
                .serviceFee(dish.getServiceFee())
                .preparationTime(dish.getPreparationTime())
                .viewCount(dish.getViewCount())
                .orderCount(dish.getOrderCount())
                .isRecommended(dish.getIsRecommended())
                .isActive(dish.getIsActive())
                .createdAt(dish.getCreatedAt())
                .updatedAt(dish.getUpdatedAt())
                .categories(categoryResponses) // Gắn list Category DTO
                .build();
    }
}
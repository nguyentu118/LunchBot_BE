package vn.codegym.lunchbot_be.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.Dish;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishDetailResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer preparationTime;
    private Integer viewCount;

    private List<DishImageDTO> images;
    private String merchantName;
    private Long merchantId;

    private List<CategoryResponse> categories;

    // ✅ Constructor nhận Dish entity
    public DishDetailResponse(Dish dish) {
        if (dish == null) {
            return;
        }

        this.id = dish.getId();
        this.name = dish.getName();
        this.description = dish.getDescription();
        this.price = dish.getPrice();
        this.discountPrice = dish.getDiscountPrice();
        this.preparationTime = dish.getPreparationTime();
        this.viewCount = dish.getViewCount();

        this.images = parseImagesToDTO(dish.getImagesUrls());

        if (dish.getMerchant() != null) {
            this.merchantName = dish.getMerchant().getRestaurantName();
            this.merchantId = dish.getMerchant().getId();
        }
        if (dish.getCategories() != null && !dish.getCategories().isEmpty()) {
            this.categories = dish.getCategories().stream()
                    .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getSlug()))
                    .collect(Collectors.toList());
        } else {
            this.categories = new ArrayList<>();
        }
    }


    private static List<DishImageDTO> parseImagesToDTO(String imagesUrlsJson) {

        if (imagesUrlsJson == null || imagesUrlsJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String[] urls = mapper.readValue(imagesUrlsJson, String[].class);

            List<DishImageDTO> imageDTOs = new ArrayList<>();
            for (int i = 0; i < urls.length; i++) {
                imageDTOs.add(DishImageDTO.builder()
                        .id((long) i)
                        .imageUrl(urls[i])
                        .displayOrder(i + 1)
                        .isPrimary(i == 0)
                        .build());
            }

            return imageDTOs;

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
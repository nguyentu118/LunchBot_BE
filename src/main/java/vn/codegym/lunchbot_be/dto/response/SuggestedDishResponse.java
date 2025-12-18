package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;

@Data
@Builder
public class SuggestedDishResponse {
    private Long id;
    private String name;
    // Lấy ảnh đầu tiên trong JSON array
    private String imageUrl; // Ảnh đại diện món
    private String merchantName;
    private String merchantAddress; // Địa chỉ của Merchant (Nơi bán/cung cấp món ăn)
    private Integer preparationTime; // Thời gian chế biến
    private BigDecimal price; // Giá gốc
    private BigDecimal discountPrice; // Giá giảm (discountPrice)

    // Giá trị Giảm giá (% giảm giá)
    private BigDecimal discountPercentage;


    private String couponCode;

    public static SuggestedDishResponse fromEntity(Dish dish) {
        // Xử lý lấy ảnh đầu tiên từ chuỗi JSON (ví dụ đơn giản, bạn có thể cần thư viện JSON)
        String firstImageUrl = (dish.getImagesUrls() != null && dish.getImagesUrls().contains("["))
                ? dish.getImagesUrls().split("\"")[1] : null;

        // Tính toán phần trăm giảm giá (Ví dụ: (price - discountPrice) / price)
        BigDecimal percentage = BigDecimal.ZERO;
        if (dish.getPrice() != null && dish.getDiscountPrice() != null && dish.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            percentage = dish.getPrice()
                    .subtract(dish.getDiscountPrice())
                    .multiply(new BigDecimal(100))
                    .divide(dish.getPrice(), 2, BigDecimal.ROUND_HALF_UP);
        }

        return SuggestedDishResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .imageUrl(firstImageUrl)
                // Lấy địa chỉ từ Merchant
                .merchantName(dish.getMerchant() != null ? dish.getMerchant().getRestaurantName() : "Không rõ")
                .merchantAddress(dish.getMerchant() != null ? dish.getMerchant().getAddress() : "Không rõ")
                .preparationTime(dish.getPreparationTime())
                .price(dish.getPrice())
                .discountPrice(dish.getDiscountPrice())
                .discountPercentage(percentage)
                .build();
    }
}

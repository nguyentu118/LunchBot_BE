package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@Builder
public class DishDiscountResponse {
    private Long id;
    private String name;
    private String imageUrl; // Ảnh đại diện món
    private String address; // Địa chỉ của Merchant
    private Integer preparationTime; // Thời gian chế biến (phút)
    private BigDecimal originalPrice; // Giá gốc
    private BigDecimal discountedPrice; // Giá đã giảm
    private BigDecimal discountPercentage; // % giảm giá (đã tính toán)
    private String couponCode; // coupon neu co

    public static DishDiscountResponse fromEntity(Dish dish) {
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

        return DishDiscountResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .imageUrl(firstImageUrl)
                // Lấy địa chỉ từ Merchant
                .address(dish.getMerchant() != null ? dish.getMerchant().getAddress() : "Không rõ")
                .preparationTime(dish.getPreparationTime())
                .originalPrice(dish.getPrice())
                .discountedPrice(dish.getDiscountPrice())
                .discountPercentage(percentage)
                .build();
    }
}

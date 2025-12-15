package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularMerchantDto {
    private Long id;
    private String name;              // Tên nhà hàng
    private String cuisine;           // Loại món ăn (từ categories của dishes)
    private String address;           // Địa chỉ
    private String deliveryTime;      // Thời gian giao hàng ước tính
    private String priceRange;        // Khoảng giá (min - max từ dishes)
    private Double rating;            // Đánh giá trung bình
    private String reviews;           // Số lượng đánh giá (format: "2.5k+")
    private String imageUrl;          // Ảnh đại diện (từ dish đầu tiên)
    private String deliveryFee;       // Phí giao hàng
    private Integer totalOrders;      // Tổng số đơn hàng (để tính popularity)

    // Constructor từ query result (sử dụng trong JPQL query)
    public PopularMerchantDto(
            Long id,
            String name,
            String address,
            String imageUrl,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long totalOrders
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.imageUrl = imageUrl;
        this.totalOrders = totalOrders != null ? totalOrders.intValue() : 0;

        // Format price range
        if (minPrice != null && maxPrice != null) {
            this.priceRange = String.format("%,.0f₫ - %,.0f₫",
                    minPrice.doubleValue(),
                    maxPrice.doubleValue()
            );
        } else {
            this.priceRange = "Liên hệ";
        }

        // Default values
        this.deliveryTime = "20-30 phút";
        this.deliveryFee = "Miễn phí";
        this.rating = 4.5 + (Math.random() * 0.4); // Random từ 4.5 - 4.9
        this.reviews = formatReviewCount(totalOrders);
        this.cuisine = "Món Việt • Đa dạng"; // Default, sẽ update sau
    }

    /**
     * Format số lượng reviews: 1500 -> "1.5k+", 250 -> "250+"
     */
    private String formatReviewCount(Long count) {
        if (count == null || count == 0) {
            return "0";
        }
        if (count >= 1000) {
            double k = count / 1000.0;
            return String.format("%.1fk+", k);
        }
        return count + "+";
    }

    /**
     * Set cuisine từ danh sách categories
     */
    public void setCuisineFromCategories(String categories) {
        if (categories != null && !categories.isEmpty()) {
            this.cuisine = categories;
        }
    }

    /**
     * Estimate delivery time từ preparation time
     */
    public void setDeliveryTimeFromPreparationTime(Integer avgPreparationTime) {
        if (avgPreparationTime != null) {
            int min = avgPreparationTime;
            int max = avgPreparationTime + 10;
            this.deliveryTime = String.format("%d-%d phút", min, max);
        }
    }
}
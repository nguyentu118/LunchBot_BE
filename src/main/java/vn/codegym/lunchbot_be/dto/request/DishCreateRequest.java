package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class DishCreateRequest {
    @NotBlank(message = "Tên món ăn không được để trống")
    @Size(max = 255, message = "Tên món ăn không được quá 255 ký tự")
    private String name;

    @NotNull(message = "ID Merchant không được để trống")
    private Long merchantId;

    private String imagesUrls;

    @PositiveOrZero(message = "Thời gian chuẩn bị phải lớn hơn hoặc bằng 0")
    private Integer preparationTime;

    private String description;

    // Giá tiền (*) - Bắt buộc
    @NotNull(message = "Giá tiền không được để trống")
    @DecimalMin(value = "0.00", inclusive = true, message = "Giá tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = true, message = "Giá khuyến mãi phải lớn hơn hoặc bằng 0")
    private BigDecimal discountPrice;

    @DecimalMin(value = "0.00", inclusive = true, message = "Phí dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    // Tag (Category IDs) (*) - Bắt buộc
    @NotEmpty(message = "Cần chọn ít nhất một Tag (Danh mục) cho món ăn")
    private Set<Long> categoryIds;

    // Đề cử (isRecommended)
    private Boolean isRecommended = false;
}
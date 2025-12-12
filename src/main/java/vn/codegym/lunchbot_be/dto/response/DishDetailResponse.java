package vn.codegym.lunchbot_be.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DishDetailResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer preparationTime;
    private Integer viewCount;

    // Danh sách ảnh
    private List<DishImageDTO> images;

    // Thông tin cơ bản về Merchant (nếu cần)
    private Long merchantId;
    private String merchantName;
}
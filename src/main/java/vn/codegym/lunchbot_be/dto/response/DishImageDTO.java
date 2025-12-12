package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DishImageDTO {
    private Long id;
    private String imageUrl;
    private String publicId;
    private Integer displayOrder;
    private Boolean isPrimary;
}

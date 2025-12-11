package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.Category;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;

    public static CategoryResponse fromEntity(Category category) {
        if (category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
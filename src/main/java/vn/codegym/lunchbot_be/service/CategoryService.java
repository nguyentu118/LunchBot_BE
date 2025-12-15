package vn.codegym.lunchbot_be.service;

import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.response.CategoryDto;
import vn.codegym.lunchbot_be.model.Category;

import java.util.List;

@Service
public interface CategoryService {
    List<Category> findAllCategories();

    List<CategoryDto> findAllCategoriesWithDishes();
}

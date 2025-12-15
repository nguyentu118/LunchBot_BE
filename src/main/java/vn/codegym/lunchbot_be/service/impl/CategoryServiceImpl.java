package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.response.CategoryDto;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.repository.CategoryRepository;
import vn.codegym.lunchbot_be.service.CategoryService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> findAllCategories() {
        // Có thể thêm logic caching hoặc filtering nếu cần thiết
        return categoryRepository.findAll();
    }

    @Override
    public List<CategoryDto> findAllCategoriesWithDishes() {
        // 1. Lấy tất cả Categories (tải kèm Dishes để tính count)
        List<Category> categories = categoryRepository.findAllCategoriesWithDishes();

        // 2. Chuyển đổi từ Entity sang DTO
        return categories.stream()
                .map(CategoryDto::fromEntity) // ⭐ Sử dụng hàm chuyển đổi từ DTO
                .collect(Collectors.toList());
    }
}



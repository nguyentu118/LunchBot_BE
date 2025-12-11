package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.repository.CategoryRepository;
import vn.codegym.lunchbot_be.service.CategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> findAllCategories() {
        // Có thể thêm logic caching hoặc filtering nếu cần thiết
        return categoryRepository.findAll();
    }
}

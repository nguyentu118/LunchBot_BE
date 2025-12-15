package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.dto.response.CategoryDto;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.service.CategoryService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping("")
    public ResponseEntity<List<Category>> getAllCategories() {
        try {
            List<Category> categories = categoryService.findAllCategories();

            if (categories.isEmpty()) {
                // Trả về 204 No Content nếu không có danh mục nào (Tùy chọn: có thể trả về 200 [] rỗng)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            return new ResponseEntity<>(categories, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/with-dishes")
    public ResponseEntity<List<CategoryDto>> getAllCategoriesWithDishes() {
        try{
            List<CategoryDto> categories = categoryService.findAllCategoriesWithDishes();
            if (categories.isEmpty()) {
                // Trả về 204 No Content nếu không có danh mục nào (Tùy chọn: có thể trả về 200 [] rỗng)
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(categories, HttpStatus.OK);
        }catch (Exception e){
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

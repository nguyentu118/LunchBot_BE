package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.response.DishResponse;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.service.DishService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;

    @PostMapping("/create")
    public ResponseEntity<?> createDish(@Valid @RequestBody DishCreateRequest request, Authentication authentication) {
        try {
            String username = authentication.getName();
            Dish createdDish = dishService.createNewDish(request, username);

            DishResponse response = DishResponse.fromEntity(createdDish);

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            return new ResponseEntity<>("Thêm món ăn thất bại: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Thêm món ăn thất bại do lỗi hệ thống.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getMerchantDishes(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<Dish> dishes = dishService.findAllDishesByMerchantUsername(username);

            // Tạo DTO đơn giản không có nested objects
            List<Map<String, Object>> simpleDishes = dishes.stream()
                    .map(dish -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", dish.getId());
                        map.put("name", dish.getName());
                        map.put("description", dish.getDescription());
                        map.put("imagesUrls", dish.getImagesUrls());
                        map.put("price", dish.getPrice());
                        map.put("discountPrice", dish.getDiscountPrice());
                        map.put("preparationTime", dish.getPreparationTime());
                        map.put("isRecommended", dish.getIsRecommended());
                        map.put("isActive", dish.getIsActive());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(simpleDishes);

        } catch (RuntimeException e) {
            return new ResponseEntity<>("Không tìm thấy món ăn: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi hệ thống khi tải danh sách món ăn.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
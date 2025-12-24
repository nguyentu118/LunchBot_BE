package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.request.DishSearchRequest;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.service.DishService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {
    private final DishService dishService;

    private final MerchantServiceImpl merchantService;

    @GetMapping("/merchant/search")
    public ResponseEntity<?> searchMyDishes(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            Pageable pageable = PageRequest.of(page, size);

            Page<Dish> dishes = dishService.searchDishes(
                    merchantId,
                    keyword,
                    categoryId,
                    minPrice,
                    maxPrice,
                    pageable
            );

            List<Map<String, Object>> simpleDishes = dishes.getContent().stream()
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

                        // ✅ THÊM categoryIds
                        List<Long> categoryIds = dish.getCategories().stream()
                                .map(Category::getId)
                                .collect(Collectors.toList());
                        map.put("categoryIds", categoryIds);

                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "content", dishes.getContent(),
                    "totalElements", dishes.getTotalElements(),
                    "totalPages", dishes.getTotalPages(),
                    "currentPage", dishes.getNumber(),
                    "size", dishes.getSize()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

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
                        List<Long> categoryIds = dish.getCategories().stream()
                                .map(Category::getId)
                                .collect(Collectors.toList());
                        map.put("categoryIds", categoryIds);
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

    // --- GET (GET /api/dishes/{dishId}) dùng cho lấy thông tin chi tiết user---
    @GetMapping("/{dishId}")
    public ResponseEntity<DishDetailResponse> getDishDetail(@PathVariable Long dishId) {
        DishDetailResponse response = dishService.getDishDetail(dishId);
        return ResponseEntity.ok(response);
    }

    // --- GET (GET /api/dishes/info/{dishId}) dùng cho Merchant quản lý món ăn ---
    @GetMapping("/info/{dishId}")
    public ResponseEntity<?> getDish(@PathVariable Long dishId) {
        try {
            Dish dish = dishService.findDishById(dishId);
            DishResponse response = DishResponse.fromEntity(dish);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return new ResponseEntity<>("Không tìm thấy món ăn: " + e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi hệ thống khi tải chi tiết món ăn.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/{dishId}")
    public ResponseEntity<?> updateDish(@PathVariable Long dishId,
                                        @Valid @RequestBody DishCreateRequest request,
                                        Authentication authentication) {
        try {
            String username = authentication.getName();
            Dish updatedDish = dishService.updateDish(dishId, request, username);

            DishResponse response = DishResponse.fromEntity(updatedDish);

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>("Cập nhật món ăn thất bại: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Cập nhật món ăn thất bại do lỗi hệ thống.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // --- DELETE (DELETE /api/dishes/{dishId}) ---
    @DeleteMapping("/{dishId}")
    public ResponseEntity<?> deleteDish(@PathVariable Long dishId, Authentication authentication) {
        try {
            String username = authentication.getName();
            dishService.deleteDish(dishId, username);

            // Trả về 200 OK với thông báo thành công
            return new ResponseEntity<>("Món ăn đã được xóa thành công.", HttpStatus.OK);

        } catch (DataIntegrityViolationException e) {
            // Trả về mã lỗi 409 Conflict
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Món ăn đang được đặt trong đơn hàng hoặc giỏ hàng, không thể xóa!"));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Xóa món ăn thất bại: " + e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggestedDishes() {
        try {
            // 1. Lấy danh sách món ăn gợi ý từ Service
            List<SuggestedDishResponse> suggestedDishes = dishService.getTopSuggestedDishes();

            // 2. Trả về kết quả
            return ResponseEntity.ok(suggestedDishes);

        } catch (Exception e) {
            // Log lỗi chi tiết nếu cần
            System.err.println("Lỗi khi tải danh sách món ăn gợi ý: " + e.getMessage());
            return new ResponseEntity<>("Lỗi hệ thống khi tải danh sách món ăn gợi ý.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Món ăn cùng danh mục
    @GetMapping("/{dishId}/related")
    public ResponseEntity<?> getRelatedDishesByCategory(@PathVariable Long dishId) {
        try {
            List<SuggestedDishResponse> relatedDishes = dishService.getRelatedDishesByCategory(dishId);
            return ResponseEntity.ok(relatedDishes);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi hệ thống.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Món ăn xem nhiều nhất
    @GetMapping("/most-viewed")
    public ResponseEntity<?> getMostViewedDishes() {
        try {
            List<SuggestedDishResponse> mostViewedDishes = dishService.getMostViewedDishes();
            return ResponseEntity.ok(mostViewedDishes);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi hệ thống.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
  
    @GetMapping("/top-discounts")
    public ResponseEntity<?> getTopDiscountedDishes() {
        try {
            // 1. Lấy danh sách món ăn giảm giá từ Service
            List<DishDiscountResponse> discountedDishes = dishService.getTop8MostDiscountedDishes();

            // 2. Trả về kết quả
            return ResponseEntity.ok(discountedDishes);

        } catch (Exception e) {
            System.err.println("Lỗi khi tải danh sách món ăn giảm giá: " + e.getMessage());
            return new ResponseEntity<>("Lỗi hệ thống khi tải danh sách món ăn giảm giá.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DishSearchResponse>> searchDishes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean isRecommended,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        DishSearchRequest request = DishSearchRequest.builder()
                .name(name)
                .categoryName(categoryName)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .isRecommended(isRecommended)
                .page(page)
                .size(size)
                .build();

        Page<DishSearchResponse> results = dishService.searchDishes(request);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/quick-search")
    public ResponseEntity<List<DishSearchResponse>> quickSearchDishes(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryName
    ) {
        List<DishSearchResponse> results = dishService.quickSearchDishes(name, categoryName);
        return ResponseEntity.ok(results);
    }
}
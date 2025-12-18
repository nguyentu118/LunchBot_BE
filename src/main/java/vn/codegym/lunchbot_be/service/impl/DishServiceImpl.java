package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.response.DishDetailResponse;
import vn.codegym.lunchbot_be.dto.response.DishDiscountResponse;
import vn.codegym.lunchbot_be.dto.response.SuggestedDishResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.repository.CategoryRepository;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.service.DishService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final MerchantRepository merchantRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Dish createNewDish(DishCreateRequest request, String username) {
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản này."));

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều Tag/Category không tồn tại.");
        }

        Dish newDish = Dish.builder()
                .merchant(merchant)
                .name(request.getName())
                .description(request.getDescription())
                .imagesUrls(request.getImagesUrls())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .serviceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO)
                .preparationTime(request.getPreparationTime())
                .viewCount(0)
                .orderCount(0)
                .isRecommended(request.getIsRecommended())
                .isActive(true)
                .categories(categories)
                .build();

        return dishRepository.save(newDish);
    }

    @Override
    public List<Dish> findAllDishesByMerchantUsername(String username) {
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản: " + username));
        return dishRepository.findByMerchantIdAndIsActiveTrue(merchant.getId());
    }

    @Override
    public Dish findDishById(Long dishId) {
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Món ăn không tồn tại với ID: " + dishId));
    }

    @Override
    @Transactional
    public Dish updateDish(Long dishId, DishCreateRequest request, String username) {
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản này."));

        Dish existingDish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại với ID: " + dishId));

        if (!existingDish.getMerchant().getId().equals(merchant.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật món ăn này.");
        }

        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));
        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều Tag/Category không tồn tại.");
        }

        existingDish.setName(request.getName());
        existingDish.setDescription(request.getDescription());
        existingDish.setImagesUrls(request.getImagesUrls());
        existingDish.setPrice(request.getPrice());
        existingDish.setDiscountPrice(request.getDiscountPrice());
        existingDish.setServiceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO);
        existingDish.setPreparationTime(request.getPreparationTime());
        existingDish.setIsRecommended(request.getIsRecommended());
        existingDish.setCategories(categories);

        return dishRepository.save(existingDish);
    }

    @Override
    @Transactional
    public void deleteDish(Long dishId, String username) {
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại"));

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại"));

        if (!dish.getMerchant().getId().equals(merchant.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa món này");
        }

        // ✅ KIỂM TRA có đơn hàng đang pending/processing không
        Long pendingOrderCount = orderRepository.countPendingOrdersByDishId(dishId);

        if (pendingOrderCount > 0) {
            throw new RuntimeException(
                    "Không thể xóa món ăn này vì đang có " + pendingOrderCount +
                            " đơn hàng chưa hoàn thành"
            );
        }

        // ✅ SOFT DELETE - Chỉ ẩn món khỏi danh sách, không ảnh hưởng đơn hàng cũ
        dish.setIsActive(false);
        dishRepository.save(dish);
    }

    @Override
    @Transactional
    public DishDetailResponse getDishDetail(Long dishId) {

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn với ID: " + dishId));

        if (!dish.getIsActive()) {
            throw new ResourceNotFoundException("Món ăn không còn khả dụng");
        }
        // Tăng view count
        dish.incrementViewCount();
        dishRepository.save(dish);
        DishDetailResponse response = new DishDetailResponse(dish);
        return response;
    }

    @Override
    public List<SuggestedDishResponse> getTopSuggestedDishes() {
        List<Dish> suggestedDishes = dishRepository.findTop8SuggestedDishes();
        return suggestedDishes.stream()
                .map(SuggestedDishResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SuggestedDishResponse> getRelatedDishesByCategory(Long dishId) {
        Dish currentDish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));

        // ✅ Lấy danh sách categories (Many-to-Many)
        Set<Category> categories = currentDish.getCategories();

        // Nếu món ăn không có category nào
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }

        // ✅ Lấy category đầu tiên (hoặc có thể lấy tất cả)
        Category primaryCategory = categories.iterator().next();

        // ✅ Tìm các dish khác có cùng category này
        List<Dish> relatedDishes = dishRepository
                .findByCategoriesContainingAndIsActiveTrueAndIdNot(primaryCategory, dishId, PageRequest.of(0, 8))
                .getContent();

        return relatedDishes.stream()
                .map(SuggestedDishResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<SuggestedDishResponse> getMostViewedDishes() {
        List<Dish> mostViewedDishes = dishRepository
                .findByIsActiveTrueOrderByViewCountDesc(PageRequest.of(0, 8))
                .getContent();

        return mostViewedDishes.stream()
                .map(SuggestedDishResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishDiscountResponse> getTop8MostDiscountedDishes() {
        List<Dish> discountedDishes = dishRepository.findTop8MostDiscountedDishes();

        // 2. Ánh xạ từ List<Dish> sang List<DishDiscountResponse> (DTO)
        return discountedDishes.stream()
                .map(DishDiscountResponse::fromEntity) // Dùng hàm static builder trong DTO
                .collect(Collectors.toList());
    }

    @Override
    public Page<Dish> searchDishes(Long merchantId, String keyword, Long categoryId,
                                   BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return dishRepository.searchDishes(merchantId, keyword, categoryId, minPrice, maxPrice, pageable);
    }

}
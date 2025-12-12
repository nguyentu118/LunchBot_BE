package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.response.DishDetailResponse;
import vn.codegym.lunchbot_be.dto.response.DishImageDTO;
import vn.codegym.lunchbot_be.dto.response.DishSimpleResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.repository.CategoryRepository;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.DishService;

import java.math.BigDecimal;
import java.util.Collections;
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


    @Override
    @Transactional
    public Dish createNewDish(DishCreateRequest request, String username) {
        // 1. TÌM MERCHANT AN TOÀN - FIX LỖI TÊN HÀM
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản này."));


        // 2. Tìm Categories/Tags (Giữ nguyên)
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));

        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều Tag/Category không tồn tại.");
        }

        // 3. Tạo Entity Dish từ Request DTO (Giữ nguyên)
        Dish newDish = Dish.builder()
                .merchant(merchant)
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .imagesUrls(request.getImagesUrls())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                // Phí dịch vụ mặc định là 0.00 nếu request gửi lên null
                .serviceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO)
                .preparationTime(request.getPreparationTime())
                // Các giá trị mặc định theo yêu cầu:
                .viewCount(0)
                .orderCount(0)
                .isRecommended(request.getIsRecommended())
                .isActive(true) // Mặc định là Active
                .categories(categories) // Thêm Tags/Categories
                .build();

        // 4. Lưu vào Database
        return dishRepository.save(newDish);
    }

    @Override
    public List<Dish> findAllDishesByMerchantUsername(String username) {
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản: " + username));

        // Yêu cầu: DishRepository phải có hàm List<Dish> findAllByMerchant(Merchant merchant);
        // Hoặc List<Dish> findAllByMerchantId(Long merchantId);
        return dishRepository.findByMerchantId(merchant.getId());
    }

    @Override
    public Dish findDishById(Long dishId) {
        return dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại với ID: " + dishId));
    }

    @Override
    @Transactional
    public Dish updateDish(Long dishId, DishCreateRequest request, String username) {
        // 1. TÌM MERCHANT AN TOÀN
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản này."));

        // 2. TÌM MÓN ĂN CẦN CẬP NHẬT
        Dish existingDish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại với ID: " + dishId));

        // 3. KIỂM TRA QUYỀN
        if (!existingDish.getMerchant().getId().equals(merchant.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật món ăn này.");
        }

        // 4. Tìm Categories/Tags mới
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(request.getCategoryIds()));

        if (categories.size() != request.getCategoryIds().size()) {
            throw new RuntimeException("Một hoặc nhiều Tag/Category không tồn tại.");
        }

        // 5. CẬP NHẬT THÔNG TIN
        existingDish.setName(request.getName());
        existingDish.setAddress(request.getAddress());
        existingDish.setDescription(request.getDescription());
        existingDish.setImagesUrls(request.getImagesUrls());
        existingDish.setPrice(request.getPrice());
        existingDish.setDiscountPrice(request.getDiscountPrice());
        existingDish.setServiceFee(request.getServiceFee() != null ? request.getServiceFee() : BigDecimal.ZERO);
        existingDish.setPreparationTime(request.getPreparationTime());
        existingDish.setIsRecommended(request.getIsRecommended());
        existingDish.setCategories(categories);

        // 6. Lưu vào Database
        return dishRepository.save(existingDish);
    }

    @Override
    @Transactional
    public void deleteDish(Long dishId, String username) {
        // 1. TÌM MERCHANT AN TOÀN
        Merchant merchant = merchantRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Merchant không tồn tại với tài khoản này."));

        // 2. TÌM MÓN ĂN CẦN XÓA
        Dish existingDish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("Món ăn không tồn tại với ID: " + dishId));

        // 3. KIỂM TRA QUYỀN
        if (!existingDish.getMerchant().getId().equals(merchant.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa món ăn này.");
        }

        // 4. THỰC HIỆN XÓA
        dishRepository.delete(existingDish);
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

        return mapToDishDetailResponse(dish);
    }

    private DishDetailResponse mapToDishDetailResponse(Dish dish) {
        return DishDetailResponse.builder()
                .id(dish.getId())
                .name(dish.getName())
                .description(dish.getDescription())
                .price(dish.getPrice())
                .discountPrice(dish.getDiscountPrice())
                .preparationTime(dish.getPreparationTime())
                .viewCount(dish.getViewCount())
                .images(dish.getImages().stream()
                        .map(image -> DishImageDTO.builder()
                                .id(image.getId())
                                .imageUrl(image.getImageUrl())
                                .publicId(image.getPublicId())
                                .displayOrder(image.getDisplayOrder())
                                .isPrimary(image.getIsPrimary())
                                .build())
                        .collect(Collectors.toList()))
                .merchantId(dish.getMerchant().getId())
                .merchantName(dish.getMerchant().getRestaurantName())
                .build();
    }
}
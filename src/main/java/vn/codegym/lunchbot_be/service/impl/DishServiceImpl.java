package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.repository.CategoryRepository;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.DishService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
}
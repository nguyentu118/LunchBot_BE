package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.dto.response.MerchantResponseDTO;
import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;
import vn.codegym.lunchbot_be.exception.InvalidOperationException;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.MerchantService;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final UserRepository userRepository;

    private final MerchantRepository merchantRepository;

    public Long getMerchantIdByUserId(Long userId) {
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant với User ID: " + userId));
        return merchant.getId();
    }

    @Transactional
    public Merchant updateMerchanntInfo(Long userId, MerchantUpdateRequest request) {

        LocalTime openTime;
        LocalTime closeTime;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Merchant merchant = user.getMerchant();

        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant không tồn tại");
        }

        try {
            openTime = LocalTime.parse(request.getOpenTime());
            closeTime = LocalTime.parse(request.getCloseTime());

        } catch (DateTimeParseException e) {
            throw new InvalidOperationException("Định dạng giờ mở/đóng không hợp lệ. Vui lòng sử dụng định dạng HH:mm:ss.");
        }
        if (closeTime.isBefore(openTime) || closeTime.equals(openTime)) {
            throw new InvalidOperationException("Giờ đóng cửa phải sau giờ mở cửa.");
        }

        merchant.setRestaurantName(request.getRestaurantName());
        merchant.setAddress(request.getAddress());
        merchant.setOpenTime(openTime);
        merchant.setCloseTime(closeTime);

        return merchantRepository.save(merchant);

    }

    public MerchantResponseDTO getMerchantProfileByEmail(String email) {
        // 1. Tìm User và Merchant dựa trên email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Merchant merchant = user.getMerchant();
        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant profile not found for this user");
        }

        MerchantResponseDTO response = getMerchantResponseDTO(user, merchant);

        return response;
    }

    private static MerchantResponseDTO getMerchantResponseDTO(User user, Merchant merchant) {
        MerchantResponseDTO response = new MerchantResponseDTO();

        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());

        response.setRestaurantName(merchant.getRestaurantName());
        response.setAddress(merchant.getAddress());

        response.setOpenTime(
                merchant.getOpenTime() != null ? merchant.getOpenTime().toString() : ""
        );
        response.setCloseTime(
                merchant.getCloseTime() != null ? merchant.getCloseTime().toString() : ""
        );
        return response;
    }

    // ⭐ NEW METHOD: Lấy danh sách nhà hàng nổi tiếng
    @Override
    public List<PopularMerchantDto> getPopularMerchants(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        try {
            // Thử query DTO trực tiếp trước
            List<PopularMerchantDto> merchants = merchantRepository.findPopularMerchants(pageable);

            // Enrich data: thêm cuisine từ categories
            merchants.forEach(dto -> {
                enrichMerchantData(dto);
            });

            return merchants;

        } catch (Exception e) {
            // Nếu query DTO không work, dùng alternative query
            System.out.println("⚠️ Query DTO failed, using alternative: " + e.getMessage());
            return getPopularMerchantsAlternative(limit);
        }
    }

    /**
     * Alternative method: Manual mapping từ Entity sang DTO
     */
    private List<PopularMerchantDto> getPopularMerchantsAlternative(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Merchant> merchants = merchantRepository.findApprovedMerchantsWithActiveDishes(pageable);

        return merchants.stream()
                .map(this::mapToPopularMerchantDto)
                .collect(Collectors.toList());
    }

    /**
     * Map Merchant Entity sang PopularMerchantDto
     */
    private PopularMerchantDto mapToPopularMerchantDto(Merchant merchant) {
        List<Dish> activeDishes = merchant.getDishes().stream()
                .filter(Dish::getIsActive)
                .collect(Collectors.toList());

        if (activeDishes.isEmpty()) {
            return null; // Skip merchant không có món
        }

        // Tính toán dữ liệu
        BigDecimal minPrice = activeDishes.stream()
                .map(Dish::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = activeDishes.stream()
                .map(Dish::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        Long totalOrders = activeDishes.stream()
                .mapToLong(Dish::getOrderCount)
                .sum();

        // Lấy ảnh đầu tiên
        String imageUrl = activeDishes.stream()
                .flatMap(dish -> dish.getImages().stream())
                .findFirst()
                .map(image -> image.getImageUrl())
                .orElse(null);

        PopularMerchantDto dto = new PopularMerchantDto(
                merchant.getId(),
                merchant.getRestaurantName(),
                merchant.getAddress(),
                imageUrl,
                minPrice,
                maxPrice,
                totalOrders
        );

        // Enrich thêm data
        enrichMerchantData(dto);

        return dto;
    }

    /**
     * Enrich merchant data với thông tin bổ sung
     */
    private void enrichMerchantData(PopularMerchantDto dto) {
        // Lấy cuisine từ categories của dishes (mock data cho đơn giản)
        // TODO: Query thực tế từ DB nếu cần
        String[] cuisineOptions = {
                "Món Việt • Phở • Bún",
                "Fastfood • Gà rán • Burger",
                "Món Thái • Lẩu • Hải sản",
                "Nhật Bản • Sushi • Sashimi",
                "Ý • Pizza • Pasta",
                "Món Việt • Cơm tấm • Sườn",
                "Đồ uống • Trà sữa • Smoothie"
        };

        int index = (int) (dto.getId() % cuisineOptions.length);
        dto.setCuisineFromCategories(cuisineOptions[index]);
    }
  
    public Long getCurrentMerchantId() {
        // 1. Lấy Email từ SecurityContext
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa đăng nhập");
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        // 2. Tìm User từ Email để lấy ID
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với email: " + email));

        // 3. ✅ Gọi method findByUserId như bạn muốn
        Merchant merchant = merchantRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Tài khoản này chưa đăng ký thông tin Cửa hàng (Merchant)"));

        return merchant.getId();
    }
}

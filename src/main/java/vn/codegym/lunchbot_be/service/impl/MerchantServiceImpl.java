package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // ⭐ UPDATED METHOD: Lấy danh sách nhà hàng nổi tiếng
    @Override
    public List<PopularMerchantDto> getPopularMerchants(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        try {
            // Query DTO trực tiếp
            List<PopularMerchantDto> merchants = merchantRepository.findPopularMerchants(pageable);

            // ✅ Enrich data: thêm REAL categories và images từ DB
            merchants.forEach(dto -> {
                enrichMerchantDataFromDB(dto);
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

        // ✅ Enrich thêm data từ DB
        enrichMerchantDataFromDB(dto);

        return dto;
    }

    // Trong MerchantServiceImpl.java
    private void enrichMerchantDataFromDB(PopularMerchantDto dto) {
        try {
            // 1️⃣ Lấy CATEGORIES THỰC TẾ từ DB
            List<String> categoryNames = merchantRepository.findCategoryNamesByMerchantId(dto.getId());

            if (!categoryNames.isEmpty()) {
                // Format: "Món Việt • Phở • Bún" (lấy tối đa 3 categories)
                String cuisineText = categoryNames.stream()
                        .limit(3) // Chỉ lấy 3 categories đầu
                        .collect(Collectors.joining(" • "));

                dto.setCuisineFromCategories(cuisineText);

                System.out.println("✅ Merchant #" + dto.getId() + " - Categories: " + cuisineText);
            } else {
                // Fallback nếu không có category
                dto.setCuisineFromCategories("Đa dạng món ăn");
                System.out.println("⚠️ Merchant #" + dto.getId() + " - No categories found");
            }

            // 2️⃣ Lấy ẢNH THỰC TẾ (Logic mới)
            if (dto.getImageUrl() == null || dto.getImageUrl().isEmpty()) {

                // Gọi Native Query mới
                List<String> rawImages = merchantRepository.findRawImageJsonByMerchantId(dto.getId());

                if (!rawImages.isEmpty()) {
                    String rawJson = rawImages.get(0); // Nhận được chuỗi: ["http://..."]

                    // Hàm làm sạch chuỗi (bỏ ngoặc [], dấu ")
                    String cleanUrl = parseImageJson(rawJson);

                    dto.setImageUrl(cleanUrl);
                    System.out.println("✅ Merchant #" + dto.getId() + " - Image found: " + cleanUrl);
                } else {
                    // Fallback
                    dto.setImageUrl("https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=400&h=300&fit=crop");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm helper để làm sạch chuỗi JSON ảnh
    private String parseImageJson(String json) {
        if (json == null) return null;
        // Xóa [, ], và "
        String clean = json.replace("[", "").replace("]", "").replace("\"", "");
        // Nếu có nhiều ảnh (phân cách dấu phẩy), lấy cái đầu tiên
        if (clean.contains(",")) {
            return clean.split(",")[0].trim();
        }
        return clean.trim();
    }

}
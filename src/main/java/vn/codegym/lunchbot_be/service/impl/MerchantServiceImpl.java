package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.BankAccountRequest;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.exception.InvalidOperationException;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.PartnerStatus;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.MerchantService;
import vn.codegym.lunchbot_be.service.PartnerNotificationService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final UserRepository userRepository;

    private final MerchantRepository merchantRepository;

    private final DishRepository dishRepository;

    private final OrderRepository orderRepository;

    private final PartnerNotificationService partnerNotificationService;

    private static final BigDecimal PARTNER_REVENUE_THRESHOLD = new BigDecimal("100000000"); // 100 triệu


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

    @Override
    public MerchantProfileResponse getMerchantById(Long id) {
        // Tìm merchant hoặc ném lỗi nếu không thấy
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng với ID: " + id));

        // Chuyển đổi từ Entity sang DTO
        return MerchantProfileResponse.builder()
                .restaurantName(merchant.getRestaurantName())
                .address(merchant.getAddress())
                .phone(merchant.getPhone())
                .avatarUrl(merchant.getAvatarUrl())
                .openTime(merchant.getOpenTime())
                .closeTime(merchant.getCloseTime())
                .build();
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

            } else {
                // Fallback nếu không có category
                dto.setCuisineFromCategories("Đa dạng món ăn");
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

    @Override
    @Transactional
    public void updateMerchantAvatar(Long userId, String avatarUrl) {
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));
        merchant.setAvatarUrl(avatarUrl);
        merchantRepository.save(merchant);
    }

    @Override
    public Merchant findByUserId(Long userId) {
        return merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhà hàng cho người dùng này."));
    }

    @Override
    public List<DishResponse> getDishesByMerchantId(Long merchantId) {
        // 1. Kiểm tra sự tồn tại của Merchant (tùy chọn nhưng nên có)
        if (!merchantRepository.existsById(merchantId)) {
            throw new ResourceNotFoundException("Nhà hàng không tồn tại với ID: " + merchantId);
        }

        // 2. Gọi Repository lấy danh sách món ăn
        List<Dish> dishes = dishRepository.findByMerchantIdAndIsActiveTrue(merchantId);

        // 3. Chuyển đổi từ Entity sang DishResponse DTO
        return dishes.stream()
                .map(dish -> DishResponse.builder()
                        .id(dish.getId())
                        .name(dish.getName())
                        .description(dish.getDescription())
                        .price(dish.getPrice())
                        .imagesUrls(dish.getImagesUrls())
                        .discountPrice(dish.getDiscountPrice())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void registerPartner(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));

        // 1. Kiểm tra trạng thái hiện tại
        if (merchant.getPartnerStatus() == PartnerStatus.PENDING) {
            throw new IllegalStateException("Bạn đã gửi yêu cầu, vui lòng chờ duyệt.");
        }
        if (merchant.getPartnerStatus() == PartnerStatus.APPROVED) {
            throw new IllegalStateException("Bạn đã là đối tác thân thiết rồi.");
        }

        // 2. Tính doanh thu THÁNG HIỆN TẠI
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal currentMonthRevenue = orderRepository.sumRevenueByMerchantAndDateRange(
                merchantId, startDate, endDate
        );

        if (currentMonthRevenue == null) {
            currentMonthRevenue = BigDecimal.ZERO;
        }

        // 3. Kiểm tra điều kiện > 100tr
        if (currentMonthRevenue.compareTo(PARTNER_REVENUE_THRESHOLD) < 0) {
            throw new IllegalStateException(
                    "Doanh thu tháng này của bạn là " +
                            String.format("%,.0f", currentMonthRevenue) + " VNĐ. " +
                            "Chưa đạt điều kiện tối thiểu 100,000,000 VNĐ để đăng ký."
            );
        }

        // 4. Cập nhật trạng thái
        merchant.setPartnerStatus(PartnerStatus.PENDING);
        merchantRepository.save(merchant);

        // ✅ 5. GỬI THÔNG BÁO CHO ADMIN
        partnerNotificationService.notifyAdminNewPartnerRequest(merchant);
    }

    @Override // Nhớ khai báo trong Interface MerchantService nữa nhé
    public BigDecimal calculateCurrentMonthRevenue(Long merchantId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal revenue = orderRepository.sumRevenueByMerchantAndDateRange(
                merchantId, startDate, endDate
        );

        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    @Override
    public List<MerchantProfileResponse> getPendingPartnerRequests() {
        List<Merchant> pendingMerchants = merchantRepository.findByPartnerStatus(PartnerStatus.PENDING);

        return pendingMerchants.stream()
                .map(merchant -> MerchantProfileResponse.builder()
                        .merchantId(merchant.getId())
                        .restaurantName(merchant.getRestaurantName())
                        .address(merchant.getAddress())
                        .phone(merchant.getPhone())
                        .partnerStatus(merchant.getPartnerStatus())
                        .avatarUrl(merchant.getAvatarUrl())
                        .openTime(merchant.getOpenTime())
                        .closeTime(merchant.getCloseTime())
                        .currentMonthRevenue(calculateCurrentMonthRevenue(merchant.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approvePartnerRequest(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));

        if (merchant.getPartnerStatus() != PartnerStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu không hợp lệ hoặc đã được xử lý.");
        }

        // 1. Cập nhật trạng thái
        merchant.setPartnerStatus(PartnerStatus.APPROVED);

        // 2. Cập nhật quyền lợi (Giảm phí sàn)
        merchant.setCommissionRate(new BigDecimal("0.005"));

        merchantRepository.save(merchant);

        // ✅ 3. GỬI THÔNG BÁO CHO MERCHANT
        partnerNotificationService.notifyMerchantPartnerApproved(merchant);
    }

    @Override
    @Transactional
    public void rejectPartnerRequest(Long merchantId, String reason) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));

        if (merchant.getPartnerStatus() != PartnerStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu không hợp lệ hoặc đã được xử lý.");
        }

        merchant.setPartnerStatus(PartnerStatus.REJECTED);
        merchantRepository.save(merchant);

        partnerNotificationService.notifyMerchantPartnerRejected(merchant, reason);
    }

    @Override
    public Page<MerchantResponseDTO> getAllMerchantsWithPagination(
            Pageable pageable,
            String keyword
    ) {
        Page<Merchant> merchantsPage;

        // Kiểm tra có keyword không
        if (keyword != null && !keyword.trim().isEmpty()) {
            // Tìm kiếm theo keyword
            merchantsPage = merchantRepository.searchMerchantsWithPagination(
                    keyword.trim(),
                    pageable
            );
        } else {
            // Lấy tất cả
            merchantsPage = merchantRepository.findAllByOrderByIdDesc(pageable);
        }

        // Chuyển đổi Page<Merchant> sang Page<MerchantResponseDTO>
        return merchantsPage.map(this::convertToDTO);
    }

    // Helper method để convert Entity sang DTO
    private MerchantResponseDTO convertToDTO(Merchant merchant) {
        return MerchantResponseDTO.builder()
                .id(merchant.getId())
                .restaurantName(merchant.getRestaurantName())
                .avatarUrl(merchant.getAvatarUrl())
                .address(merchant.getAddress())
                .phone(merchant.getPhone())
                .email(merchant.getUser() != null ? merchant.getUser().getEmail() : null)
                .openTime(merchant.getOpenTime() != null ? merchant.getOpenTime().toString() : null)
                .closeTime(merchant.getCloseTime() != null ? merchant.getCloseTime().toString() : null)
                .build();
    }

    @Override
    @Transactional
    public BankAccountResponse updateBankAccount(Long userId, BankAccountRequest request) {
        // 1. Tìm merchant
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhà hàng"));

        // 2. Validate: Không cho phép thay đổi nếu đang có yêu cầu rút tiền pending
        if (hasWithdrawalRequests(merchant.getId())) {
            throw new InvalidOperationException(
                    "Không thể cập nhật tài khoản ngân hàng khi đang có yêu cầu rút tiền chờ xử lý"
            );
        }

        // 3. Cập nhật thông tin
        merchant.setBankName(request.getBankName());
        merchant.setBankAccountNumber(request.getBankAccountNumber());
        merchant.setBankAccountHolder(request.getBankAccountHolder().toUpperCase());

        merchantRepository.save(merchant);

        // 4. Trả về response
        return BankAccountResponse.builder()
                .merchantId(merchant.getId())
                .restaurantName(merchant.getRestaurantName())
                .bankName(merchant.getBankName())
                .bankAccountNumber(merchant.getBankAccountNumber())
                .bankAccountHolder(merchant.getBankAccountHolder())
                .hasLinkedBank(true)
                .build();
    }

    @Override
    public BankAccountResponse getBankAccount(Long userId) {
        // 1. Tìm merchant
        Merchant merchant = merchantRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin nhà hàng"));

        // 2. Kiểm tra đã có tài khoản ngân hàng chưa
        boolean hasLinkedBank = merchant.getBankAccountNumber() != null
                && !merchant.getBankAccountNumber().isEmpty();

        // 3. Trả về response
        return BankAccountResponse.builder()
                .merchantId(merchant.getId())
                .restaurantName(merchant.getRestaurantName())
                .bankName(merchant.getBankName())
                .bankAccountNumber(merchant.getBankAccountNumber())
                .bankAccountHolder(merchant.getBankAccountHolder())
                .hasLinkedBank(hasLinkedBank)
                .build();
    }


    /**
     * Helper method: Kiểm tra có withdrawal request đang pending không
     */
    private boolean hasWithdrawalRequests(Long merchantId) {
        // Implement logic check trong WithdrawalRequestRepository
        // Ví dụ:
        // return withdrawalRequestRepository
        //     .existsByMerchantIdAndStatus(merchantId, WithdrawalStatus.PENDING);

        // Tạm thời return false nếu chưa có repository
        return false;
    }
}

package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.response.AddressResponse;
import vn.codegym.lunchbot_be.dto.response.CartItemDTO;
import vn.codegym.lunchbot_be.dto.response.CheckoutResponse;
import vn.codegym.lunchbot_be.model.*;
import vn.codegym.lunchbot_be.repository.*;
import vn.codegym.lunchbot_be.service.AddressService;
import vn.codegym.lunchbot_be.service.CheckoutService;
import vn.codegym.lunchbot_be.service.CouponService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressService addressService;
    private final CouponRepository couponRepository;
    private final CouponService couponService;

    // Danh sách tỉnh/thành nội thành (Hà Nội, TP.HCM, Đà Nẵng, Cần Thơ, Hải Phòng)
    private static final Set<String> INNER_CITY_PROVINCES = Set.of(
            "Hà Nội", "Thành phố Hà Nội", "Ha Noi",
            "Hồ Chí Minh", "Thành phố Hồ Chí Minh", "Ho Chi Minh", "TP.HCM", "TPHCM",
            "Đà Nẵng", "Thành phố Đà Nẵng", "Da Nang",
            "Cần Thơ", "Thành phố Cần Thơ", "Can Tho",
            "Hải Phòng", "Thành phố Hải Phòng", "Hai Phong"
    );

    private static final BigDecimal INNER_CITY_SHIPPING_FEE = new BigDecimal("15000"); // 15k
    private static final BigDecimal OUTER_CITY_SHIPPING_FEE = new BigDecimal("25000"); // 25k
    private static final BigDecimal MAX_DISCOUNT_AMOUNT = new BigDecimal("50000"); // Giảm tối đa 50k

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse getCheckoutInfo(String email) {// lấy thông tin checkout ban đầu
        // 1. Validate cart
        validateCart(email);

        User user = getUserByEmail(email);
        Cart cart = getCartByUser(user);

        // 2. Lấy thông tin merchant (từ món đầu tiên trong cart)
        CartItem firstItem = cart.getCartItems().get(0);
        Merchant merchant = firstItem.getDish().getMerchant();

        // 3. Lấy danh sách địa chỉ
        List<AddressResponse> addresses = addressService.getAllAddressesByUser(email);
        AddressResponse defaultAddress = addressService.getDefaultAddress(email);

        // 4. Map cart items sang DTO
        List<CartItemDTO> items = cart.getCartItems().stream()
                .map(this::mapCartItemToDTO)
                .collect(Collectors.toList());

        // 5. Tính toán giá
        BigDecimal itemsTotal = items.stream()
                .map(CartItemDTO::getSubtotal)// lấy subtotal của từng món
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = calculateServiceFee(itemsTotal);

        // Phí vận chuyển tính theo địa chỉ mặc định (nếu có)
        BigDecimal shippingFee = INNER_CITY_SHIPPING_FEE; // Default
        if (defaultAddress != null) {
            shippingFee = calculateShippingFee(defaultAddress.getProvince());
        }

        BigDecimal totalAmount = itemsTotal
                .add(serviceFee)
                .add(shippingFee);

        // 6. Lấy danh sách coupon khả dụng
        List<CheckoutResponse.CouponInfo> availableCoupons = getAvailableCoupons(merchant.getId(), itemsTotal);

        // 7. Tính tổng số món
        Integer totalItems = items.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        return CheckoutResponse.builder()
                .merchantId(merchant.getId())
                .merchantName(merchant.getRestaurantName())
                .merchantAddress(merchant.getAddress())
                .merchantPhone(merchant.getPhone())
                .items(items)
                .totalItems(totalItems)
                .addresses(addresses)
                .defaultAddressId(defaultAddress != null ? defaultAddress.getId() : null)
                .itemsTotal(itemsTotal)
                .discountAmount(BigDecimal.ZERO)
                .serviceFee(serviceFee)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .appliedCouponCode(null)
                .canUseCoupon(!availableCoupons.isEmpty())
                .availableCoupons(availableCoupons)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse applyDiscount(String email, String couponCode) {// áp dụng mã giảm giá
        // 1. Lấy thông tin checkout ban đầu
        CheckoutResponse checkoutInfo = getCheckoutInfo(email);

        if (couponCode == null || couponCode.trim().isEmpty()) {
            return checkoutInfo;
        }

        // 2. Validate coupon
        Coupon coupon = couponRepository.findByCodeAndMerchantId(
                couponCode.toUpperCase(),
                checkoutInfo.getMerchantId()
        ).orElse(null);

        if (coupon == null) {
            throw new RuntimeException("Mã giảm giá không tồn tại hoặc không thuộc cửa hàng này");
        }

        if (!coupon.isValid()) {
            throw new RuntimeException("Mã giảm giá đã hết hạn hoặc đã sử dụng hết");
        }

        if (checkoutInfo.getItemsTotal().compareTo(coupon.getMinOrderValue()) < 0) {
            throw new RuntimeException(
                    String.format("Đơn hàng phải từ %s đ mới được sử dụng mã này",
                            coupon.getMinOrderValue())
            );
        }

        // 3. Tính discount
        BigDecimal discountAmount = coupon.calculateDiscount(checkoutInfo.getItemsTotal());

        // 4. Áp dụng giới hạn giảm tối đa 50k
        if (discountAmount.compareTo(MAX_DISCOUNT_AMOUNT) > 0) {
            discountAmount = MAX_DISCOUNT_AMOUNT;
        }

        // 5. Tính lại tổng tiền
        BigDecimal totalAmount = checkoutInfo.getItemsTotal()
                .subtract(discountAmount)
                .add(checkoutInfo.getServiceFee())
                .add(checkoutInfo.getShippingFee());

        // 6. Cập nhật response
        checkoutInfo.setDiscountAmount(discountAmount);
        checkoutInfo.setTotalAmount(totalAmount);
        checkoutInfo.setAppliedCouponCode(couponCode.toUpperCase());

        return checkoutInfo;
    }

    @Override
    public BigDecimal calculateServiceFee(BigDecimal itemsTotal) {
        // Theo yêu cầu: Phí dịch vụ = 0đ
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateShippingFee(String province) {
        if (province == null || province.trim().isEmpty()) {
            return INNER_CITY_SHIPPING_FEE; // Default nội thành
        }

        // Chuẩn hóa tên tỉnh (xóa khoảng trắng thừa, lowercase để so sánh)
        String normalizedProvince = province.trim();

        // Kiểm tra có phải nội thành không
        boolean isInnerCity = INNER_CITY_PROVINCES.stream()
                .anyMatch(city -> normalizedProvince.equalsIgnoreCase(city) ||
                        normalizedProvince.toLowerCase().contains(city.toLowerCase()));

        return isInnerCity ? INNER_CITY_SHIPPING_FEE : OUTER_CITY_SHIPPING_FEE;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCart(String email) {
        User user = getUserByEmail(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

        // 1. Kiểm tra giỏ hàng rỗng
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống. Vui lòng thêm món ăn trước khi thanh toán.");
        }

        // 2. Kiểm tra tất cả món thuộc cùng 1 merchant
        Set<Long> merchantIds = cart.getCartItems().stream()
                .map(item -> item.getDish().getMerchant().getId())
                .collect(Collectors.toSet());

        if (merchantIds.size() > 1) {
            throw new RuntimeException(
                    "Giỏ hàng chứa món ăn từ nhiều cửa hàng. " +
                            "Mỗi đơn hàng chỉ có thể đặt từ 1 cửa hàng. " +
                            "Vui lòng xóa bớt món từ các cửa hàng khác."
            );
        }

        // 3. Kiểm tra món ăn còn active không
        for (CartItem item : cart.getCartItems()) {
            if (!item.getDish().getIsActive()) {
                throw new RuntimeException(
                        String.format("Món '%s' hiện không còn bán. Vui lòng xóa khỏi giỏ hàng.",
                                item.getDish().getName())
                );
            }
        }
    }

    // ========== HELPER METHODS ==========

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private Cart getCartByUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
    }

    private CartItemDTO mapCartItemToDTO(CartItem item) {
        String firstImage = extractFirstImageUrl(item.getDish().getImagesUrls());

        return CartItemDTO.builder()
                .id(item.getId())
                .dishId(item.getDish().getId())
                .dishName(item.getDish().getName())
                .dishImage(firstImage)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }

    private String extractFirstImageUrl(String imagesUrlsJson) {
        if (imagesUrlsJson == null || imagesUrlsJson.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = imagesUrlsJson
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .trim();

            String[] urls = cleaned.split(",");
            if (urls.length > 0) {
                return urls[0].trim();
            }
        } catch (Exception e) {
            return imagesUrlsJson;
        }

        return null;
    }

    private List<CheckoutResponse.CouponInfo> getAvailableCoupons(Long merchantId, BigDecimal orderTotal) {
        List<Coupon> coupons = couponRepository.findActiveCouponsByMerchant(merchantId, LocalDate.now());

        return coupons.stream()
                .filter(coupon -> orderTotal.compareTo(coupon.getMinOrderValue()) >= 0)
                .map(coupon -> CheckoutResponse.CouponInfo.builder()
                        .id(coupon.getId())
                        .code(coupon.getCode())
                        .description(buildCouponDescription(coupon))
                        .discountValue(coupon.getDiscountValue())
                        .discountType(coupon.getDiscountType().name())
                        .minOrderValue(coupon.getMinOrderValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String buildCouponDescription(Coupon coupon) {
        StringBuilder desc = new StringBuilder("Giảm ");

        switch (coupon.getDiscountType()) {
            case PERCENTAGE:
                desc.append(coupon.getDiscountValue()).append("%");
                break;
            case FIXED_AMOUNT:
                desc.append(coupon.getDiscountValue()).append("đ");
                break;
        }

        if (coupon.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
            desc.append(" cho đơn từ ").append(coupon.getMinOrderValue()).append("đ");
        }

        desc.append(" (Tối đa 50k)");

        return desc.toString();
    }
}
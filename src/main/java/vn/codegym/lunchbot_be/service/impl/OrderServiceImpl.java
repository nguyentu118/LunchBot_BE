package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.model.*;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import vn.codegym.lunchbot_be.repository.*;
import vn.codegym.lunchbot_be.service.AddressService;
import vn.codegym.lunchbot_be.service.CheckoutService;
import vn.codegym.lunchbot_be.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final CheckoutService checkoutService;
    private final AddressService addressService;
    private final ShippingPartnerRepository shippingPartnerRepository;

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse getCheckoutInfo(String email) {
        return checkoutService.getCheckoutInfo(email);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse applyDiscount(String email, String couponCode) {
        return checkoutService.applyDiscount(email, couponCode);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String email, CheckoutRequest request) {
        // 1. Validate cart
        checkoutService.validateCart(email);

        User user = getUserByEmail(email);
        Cart cart = getCartByUser(user);

        List<CartItem> itemsToOrder;

        // Kiểm tra dishIds trước
        if (request.getDishIds() == null || request.getDishIds().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn món ăn để đặt hàng");
        }

        // Lọc theo dishIds từ frontend
        itemsToOrder = cart.getCartItems().stream()
                .filter(item -> request.getDishIds().contains(item.getDish().getId()))
                .collect(Collectors.toList());

        if (itemsToOrder.isEmpty()) {
            throw new RuntimeException("Không tìm thấy món được chọn trong giỏ hàng");
        }

        // Validate tất cả món phải cùng 1 merchant
        long merchantCount = itemsToOrder.stream()
                .map(item -> item.getDish().getMerchant().getId())
                .distinct()
                .count();

        if (merchantCount > 1) {
            throw new RuntimeException("Vui lòng chọn món từ cùng một nhà hàng");
        }

        // 2. Validate address
        Address shippingAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ giao hàng"));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Địa chỉ giao hàng không hợp lệ");
        }

        // 3. Lấy merchant từ cart
        Merchant merchant = itemsToOrder.get(0).getDish().getMerchant();

        // 4. ✅ LẤY SHIPPING PARTNER MẶC ĐỊNH
        ShippingPartner defaultShippingPartner = shippingPartnerRepository.findAll().stream()
                .filter(ShippingPartner::getIsDefault)
                .filter(partner -> !partner.getIsLocked()) // Chỉ lấy partner không bị khóa
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác vận chuyển mặc định"));

        // 5. Tính toán giá
        BigDecimal itemsTotal = itemsToOrder.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = checkoutService.calculateServiceFee(itemsTotal);
        BigDecimal shippingFee = checkoutService.calculateShippingFee(shippingAddress.getProvince());

        // 6. ✅ TÍNH PHÍ HOA HỒNG CHO MERCHANT (dựa trên merchant commission rate)
        BigDecimal merchantCommissionRate = merchant.getCommissionRate() != null
                ? merchant.getCommissionRate()
                : BigDecimal.ZERO;
        BigDecimal merchantCommissionFee = itemsTotal
                .multiply(merchantCommissionRate)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        // 7. ✅ TÍNH PHÍ HOA HỒNG CHO SHIPPING PARTNER (dựa trên shipping partner commission rate)
        // Phí này được tính trên shipping fee
        BigDecimal shipperCommissionRate = defaultShippingPartner.getCommissionRate() != null
                ? defaultShippingPartner.getCommissionRate()
                : BigDecimal.ZERO;

        BigDecimal shipperCommissionFee = shippingFee
                .multiply(shipperCommissionRate)
                .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);

        // 8. Xử lý coupon (nếu có)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            coupon = couponRepository.findByCodeAndMerchantId(
                    request.getCouponCode().toUpperCase(),
                    merchant.getId()
            ).orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ"));

            if (!coupon.isValid()) {
                throw new RuntimeException("Mã giảm giá đã hết hạn hoặc đã sử dụng hết");
            }

            if (itemsTotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new RuntimeException(
                        String.format("Đơn hàng phải từ %s đ mới được sử dụng mã này",
                                coupon.getMinOrderValue())
                );
            }

            discountAmount = coupon.calculateDiscount(itemsTotal);

            // Áp dụng giới hạn giảm tối đa 50k
            BigDecimal maxDiscount = new BigDecimal("50000");
            if (discountAmount.compareTo(maxDiscount) > 0) {
                discountAmount = maxDiscount;
            }
        }

        // 9. Tính tổng thanh toán
        BigDecimal totalAmount = itemsTotal
                .subtract(discountAmount)
                .add(serviceFee)
                .add(shippingFee);

        // 10. Tạo order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .merchant(merchant)
                .shippingAddress(shippingAddress)
                .shippingPartner(defaultShippingPartner) // ✅ Gán shipping partner mặc định
                .commissionRate(defaultShippingPartner.getCommissionRate()) // ✅ Lưu snapshot commission rate
                .coupon(coupon)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .itemsTotal(itemsTotal)
                .discountAmount(discountAmount)
                .serviceFee(serviceFee)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .commissionFee(shipperCommissionFee) // ✅ Lưu phí hoa hồng cho shipper
                .notes(request.getNotes())
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        // 11. Tạo order items từ cart items - Lưu SNAPSHOT
        for (CartItem cartItem : itemsToOrder) {
            Dish dish = cartItem.getDish();
            String firstImage = extractFirstImageUrl(dish.getImagesUrls());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishImage(firstImage)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPrice())
                    .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .merchantId(merchant.getId())
                    .merchantName(merchant.getRestaurantName())
                    .build();
            order.getOrderItems().add(orderItem);
            dish.incrementOrderCount();
        }

        // 12. Tăng usedCount cho coupon (nếu có)
        if (coupon != null) {
            coupon.incrementUsedCount();
            couponRepository.save(coupon);
        }

        // 13. Lưu order
        Order savedOrder = orderRepository.save(order);

        // 14. Xóa các món đã đặt khỏi giỏ hàng
        for (CartItem item : itemsToOrder) {
            cart.getCartItems().remove(item);
        }
        cartRepository.save(cart);

        return mapToOrderResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(String email) {
        User user = getUserByEmail(email);

        List<Order> orders = orderRepository.findByUserId(user.getId());

        // Sắp xếp theo orderDate mới nhất
        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, Long orderId) {
        User user = getUserByEmail(email);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
        }

        return mapToOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(String email, Long orderId, String reason) {
        User user = getUserByEmail(email);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra quyền sở hữu
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
        }

        // Kiểm tra có thể hủy không
        if (!order.isCancellable()) {
            throw new RuntimeException("Đơn hàng này không thể hủy");
        }

        // Cập nhật trạng thái
        order.updateStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);

        // Hoàn lại usedCount cho coupon (nếu có)
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            coupon.setUsedCount(coupon.getUsedCount() - 1);
            couponRepository.save(coupon);
        }

        Order cancelledOrder = orderRepository.save(order);

        return mapToOrderResponse(cancelledOrder);
    }

    // ========== HELPER METHODS ==========

    /**
     * Generate order number theo format: ORD-YYYYMMDD-XXX
     * VD: ORD-20231215-001
     */
    private String generateOrderNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD-" + dateStr + "-";

        // Đếm số order trong ngày
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        List<Order> todayOrders = orderRepository.findOrdersBetweenDates(startOfDay, endOfDay);
        int orderCount = todayOrders.size() + 1;

        return prefix + String.format("%03d", orderCount);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    private Cart getCartByUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));
    }

    /**
     * Map Order entity sang OrderResponse DTO
     */
    private OrderResponse mapToOrderResponse(Order order) {
        // Map shipping address
        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();
            addressResponse = AddressResponse.builder()
                    .id(addr.getId())
                    .contactName(addr.getContactName())
                    .phone(addr.getPhone())
                    .province(addr.getProvince())
                    .district(addr.getDistrict())
                    .ward(addr.getWard())
                    .street(addr.getStreet())
                    .building(addr.getBuilding())
                    .isDefault(addr.getIsDefault())
                    .build();
            addressResponse.setFullAddress(addressResponse.buildFullAddress());
        }

        // Map order items
        List<OrderItemDTO> items = order.getOrderItems().stream()
                .map(this::mapToOrderItemDTO)
                .collect(Collectors.toList());

        Integer totalItems = items.stream()
                .mapToInt(OrderItemDTO::getQuantity)
                .sum();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .merchantId(order.getMerchant().getId())
                .merchantName(order.getMerchant().getRestaurantName())
                .merchantAddress(order.getMerchant().getAddress())
                .merchantPhone(order.getMerchant().getPhone())
                .shippingAddress(addressResponse)
                .shippingPartnerName(order.getShippingPartner() != null ? order.getShippingPartner().getName() : null) // ✅ Thêm tên shipper
                .items(items)
                .totalItems(totalItems)
                .itemsTotal(order.getItemsTotal())
                .discountAmount(order.getDiscountAmount())
                .serviceFee(order.getServiceFee())
                .shippingFee(order.getShippingFee())
                .commissionFee(order.getCommissionFee()) // ✅ Thêm phí hoa hồng shipper
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCoupon() != null ? order.getCoupon().getCode() : null)
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .expectedDeliveryTime(order.getExpectedDeliveryTime())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .build();
    }

    private OrderItemDTO mapToOrderItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .dishId(item.getDishId())
                .dishName(item.getDishName())
                .dishImage(item.getDishImage())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
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
}
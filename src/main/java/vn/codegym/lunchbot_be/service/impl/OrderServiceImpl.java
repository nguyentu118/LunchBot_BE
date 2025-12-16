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

        // 2. Validate address
        Address shippingAddress = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ giao hàng"));

        if (!shippingAddress.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Địa chỉ giao hàng không hợp lệ");
        }

        // 3. Lấy merchant từ cart
        Merchant merchant = cart.getCartItems().get(0).getDish().getMerchant();

        // 4. Tính toán giá
        BigDecimal itemsTotal = cart.getCartItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = checkoutService.calculateServiceFee(itemsTotal);
        BigDecimal shippingFee = checkoutService.calculateShippingFee(shippingAddress.getProvince());

        // 5. Xử lý coupon (nếu có)
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

        // 6. Tính tổng thanh toán
        BigDecimal totalAmount = itemsTotal
                .subtract(discountAmount)
                .add(serviceFee)
                .add(shippingFee);

        // 7. Tạo order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .merchant(merchant)
                .shippingAddress(shippingAddress)
                .coupon(coupon)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .itemsTotal(itemsTotal)
                .discountAmount(discountAmount)
                .serviceFee(serviceFee)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .notes(request.getNotes())
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        // 8. Tạo order items từ cart items
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .dish(cartItem.getDish())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPrice())
                    .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();

            order.getOrderItems().add(orderItem);

            // Tăng orderCount cho dish
            cartItem.getDish().incrementOrderCount();
        }

        // 9. Tăng usedCount cho coupon (nếu có)
        if (coupon != null) {
            coupon.incrementUsedCount();
            couponRepository.save(coupon);
        }

        // 10. Lưu order
        Order savedOrder = orderRepository.save(order);

        // 11. Xóa cart sau khi đặt hàng thành công
        cart.clear();
        cartRepository.save(cart);

        // 12. Map sang OrderResponse
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
                .items(items)
                .totalItems(totalItems)
                .itemsTotal(order.getItemsTotal())
                .discountAmount(order.getDiscountAmount())
                .serviceFee(order.getServiceFee())
                .shippingFee(order.getShippingFee())
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
        String firstImage = extractFirstImageUrl(item.getDish().getImagesUrls());

        return OrderItemDTO.builder()
                .id(item.getId())
                .dishId(item.getDish().getId())
                .dishName(item.getDish().getName())
                .dishImage(firstImage)
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
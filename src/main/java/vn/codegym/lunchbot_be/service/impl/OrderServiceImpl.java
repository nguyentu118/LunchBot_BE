package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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

        List<CartItem> itemsToOrder;

        // ✅ SỬA: Kiểm tra dishIds trước
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

        // ✅ SỬA: Validate tất cả món phải cùng 1 merchant
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

        // 4. Tính toán giá
        BigDecimal itemsTotal = itemsToOrder.stream()
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

        // 8. Tạo order items từ cart items - ✅ LƯU SNAPSHOT
        for (CartItem cartItem : itemsToOrder) {
            Dish dish = cartItem.getDish();
            String firstImage = extractFirstImageUrl(dish.getImagesUrls());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .dishId(dish.getId())  // ✅ Chỉ lưu ID
                    .dishName(dish.getName())  // ✅ Snapshot tên
                    .dishImage(firstImage)  // ✅ Snapshot ảnh
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getPrice())
                    .totalPrice(cartItem.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .merchantId(merchant.getId())  // ✅ Lưu merchant info
                    .merchantName(merchant.getRestaurantName())
                    .build();
            order.getOrderItems().add(orderItem);
            dish.incrementOrderCount();
        }

        // 9. Tăng usedCount cho coupon (nếu có)
        if (coupon != null) {
            coupon.incrementUsedCount();
            couponRepository.save(coupon);
        }

        // 10. Lưu order
        Order savedOrder = orderRepository.save(order);

        // 11. Xóa các món đã đặt khỏi giỏ hàng
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

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByMerchant(Long merchantId, OrderStatus status) {
        // 1. Nếu có status thì lọc, không thì lấy hết
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByMerchantIdAndStatus(merchantId, status);
        } else {
            orders = orderRepository.findByMerchantId(merchantId);
        }

        // 2. Sắp xếp đơn mới nhất lên đầu
        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long merchantId, Long orderId, OrderStatus newStatus) {
        // 1. Tìm đơn hàng
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 2. Validate: Đơn hàng có thuộc về merchant này không?
        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đơn hàng này");
        }

        // 3. Validate logic chuyển trạng thái (State Transition)
        // Ví dụ: Không thể chuyển từ CANCELLED về PENDING
        validateStatusTransition(order.getStatus(), newStatus);

        // 4. Cập nhật
        order.updateStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        return mapToOrderResponse(savedOrder);
    }

    // Thêm method này vào OrderServiceImpl.java

    @Override
    @Transactional(readOnly = true)
    public OrderStatisticsResponse getOrderStatisticsByMerchant(Long merchantId) {
        // Khởi tạo response với giá trị mặc định = 0
        OrderStatisticsResponse stats = OrderStatisticsResponse.builder()
                .pendingCount(0L)
                .confirmedCount(0L)
                .processingCount(0L)
                .readyCount(0L)
                .deliveringCount(0L)
                .completedCount(0L)
                .cancelledCount(0L)
                .todayOrders(0L)
                .build();

        // Đếm số đơn theo từng trạng thái
        stats.setPendingCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.PENDING));
        stats.setConfirmedCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.CONFIRMED));
        stats.setProcessingCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.PROCESSING));
        stats.setReadyCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.READY));
        stats.setDeliveringCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.DELIVERING));
        stats.setCompletedCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.COMPLETED));
        stats.setCancelledCount(orderRepository.countByMerchantIdAndStatus(merchantId, OrderStatus.CANCELLED));

        // Đếm đơn hôm nay
        stats.setTodayOrders(orderRepository.getTodayOrderCount(merchantId));

        // Tính tổng và đơn đang xử lý
        stats.calculateTotal();

        return stats;
    }

    // Thêm method này vào class OrderServiceImpl
    @Override
    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getRevenueStatistics(
            Long merchantId,
            String timeRange,
            Integer week,
            Integer month,
            Integer quarter,
            Integer year,
            int page,
            int size) {

        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        // Nếu không truyền timeRange → mặc định là MONTH (hiện tại)
        if (timeRange == null || timeRange.trim().isEmpty()) {
            timeRange = "MONTH";
        }

        // Nếu không truyền năm → dùng năm hiện tại
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        // ✅ XỬ LÝ LOGIC THEO TỪNG LOẠI THỐNG KÊ
        switch (timeRange.toUpperCase()) {

            case "WEEK":
                // ✅ Nếu có truyền week → lấy tuần cụ thể
                if (week != null && week >= 1 && week <= 53) {
                    // Tính ngày đầu tiên của tuần cụ thể
                    LocalDate jan1 = LocalDate.of(year, 1, 1);
                    // ISO Week: Thứ 2 là bắt đầu tuần
                    LocalDate firstDayOfWeek = jan1.with(
                            java.time.temporal.WeekFields.ISO.weekOfYear(), week
                    ).with(java.time.DayOfWeek.MONDAY);

                    startDate = firstDayOfWeek.atStartOfDay();
                    endDate = firstDayOfWeek.plusDays(6).atTime(23, 59, 59);
                } else {
                    // Mặc định: tuần hiện tại
                    startDate = LocalDate.now()
                            .with(java.time.DayOfWeek.MONDAY)
                            .atStartOfDay();
                    endDate = LocalDate.now()
                            .with(java.time.DayOfWeek.SUNDAY)
                            .atTime(23, 59, 59);
                }
                break;

            case "MONTH":
                // ✅ Nếu có truyền month → lấy tháng cụ thể
                if (month != null && month >= 1 && month <= 12) {
                    startDate = LocalDate.of(year, month, 1).atStartOfDay();
                    // Lấy ngày cuối cùng của tháng
                    YearMonth yearMonth = YearMonth.of(year, month);
                    endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
                } else {
                    // Mặc định: tháng hiện tại
                    startDate = LocalDate.now()
                            .withDayOfMonth(1)
                            .atStartOfDay();
                    YearMonth yearMonth = YearMonth.now();
                    endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
                }
                break;

            case "QUARTER":
                // ✅ Nếu có truyền quarter → lấy quý cụ thể
                if (quarter != null && quarter >= 1 && quarter <= 4) {
                    int firstMonth = (quarter - 1) * 3 + 1;
                    startDate = LocalDate.of(year, firstMonth, 1).atStartOfDay();

                    int lastMonth = quarter * 3;
                    YearMonth lastMonthOfQuarter = YearMonth.of(year, lastMonth);
                    endDate = lastMonthOfQuarter.atEndOfMonth().atTime(23, 59, 59);
                } else {
                    // Mặc định: quý hiện tại
                    LocalDate now = LocalDate.now();
                    int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                    int firstMonth = (currentQuarter - 1) * 3 + 1;
                    startDate = LocalDate.of(now.getYear(), firstMonth, 1)
                            .atStartOfDay();

                    int lastMonth = currentQuarter * 3;
                    YearMonth lastMonthOfQuarter = YearMonth.of(now.getYear(), lastMonth);
                    endDate = lastMonthOfQuarter.atEndOfMonth().atTime(23, 59, 59);
                }
                break;

            case "YEAR":
                // ✅ Lấy toàn bộ năm
                startDate = LocalDate.of(year, 1, 1).atStartOfDay();
                endDate = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
                break;

            default:
                // Mặc định: tháng hiện tại
                startDate = LocalDate.now()
                        .withDayOfMonth(1)
                        .atStartOfDay();
                YearMonth yearMonth = YearMonth.now();
                endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        }

        // ✅ QUERY DATABASE
        BigDecimal revenue = orderRepository.sumRevenueByDateRange(
                merchantId, startDate, endDate
        );
        if (revenue == null) revenue = BigDecimal.ZERO;

        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<Order> orderPage = orderRepository.findOrdersByDateRange(
                merchantId, startDate, endDate, pageable
        );

        Page<OrderResponse> orderResponsePage = orderPage.map(this::mapToOrderResponse);

        // ✅ BUILD RESPONSE
        return RevenueStatisticsResponse.builder()
                .totalRevenue(revenue)
                .totalOrders(orderPage.getTotalElements())
                .orders(orderResponsePage)
                .timeRange(timeRange)
                .startDate(startDate)      // ✅ THÊM để frontend biết khoảng nào
                .endDate(endDate)
                .build();
    }

    @Override
    public Page<OrderResponse> getOrdersByDish(Long merchantId, Long dishId, int page, int size) {
        // Query với phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        // Lấy danh sách đơn hàng chứa dishId
        Page<Order> orderPage = orderRepository.findOrdersByDishId(merchantId, dishId, pageable);
        // Map sang OrderResponse
        return orderPage.map(this::mapToOrderResponse);
    }


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

                .customerName(order.getUser().getFullName())           // Lấy từ User entity
                .customerPhone(order.getUser().getPhone())         // Optional: Lấy phone
                // ========================================

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
        // Không cần hàm extractFirstImageUrl nữa vì dishImage trong DB đã lưu link ảnh rồi
        return OrderItemDTO.builder()
                .id(item.getId())
                .dishId(item.getDishId())       // Lấy từ snapshot
                .dishName(item.getDishName())   // Lấy từ snapshot
                .dishImage(item.getDishImage()) // Lấy từ snapshot
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

    // Hàm phụ trợ để kiểm tra logic chuyển trạng thái hợp lệ
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã kết thúc");
        }
    }
}

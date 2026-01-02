package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.request.SepayWebhookDTO;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.*;
import vn.codegym.lunchbot_be.model.enums.CancelledBy;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import vn.codegym.lunchbot_be.repository.*;
import vn.codegym.lunchbot_be.service.CheckoutService;
import vn.codegym.lunchbot_be.service.OrderNotificationService;
import vn.codegym.lunchbot_be.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;
    private final CouponRepository couponRepository;
    private final CheckoutService checkoutService;
    private final ShippingPartnerRepository shippingPartnerRepository;
    private final ShippingServiceImpl shippingService;
    private final OrderNotificationService orderNotificationService;
    private final RefundServiceImpl refundService;


    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse getCheckoutInfo(String email, List<Long> selectedDishIds) {
        return checkoutService.getCheckoutInfo(email, selectedDishIds);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutResponse applyDiscount(String email, String couponCode, List<Long> selectedDishIds) {
        return checkoutService.applyDiscount(email, couponCode, selectedDishIds);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(String email, CheckoutRequest request) {
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        Long actualShippingFee = shippingService.calculateGhnFee(address);
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
                .map(item -> {
                    BigDecimal discountPrice = item.getDish().getDiscountPrice() != null
                            ? item.getDish().getDiscountPrice()
                            : item.getPrice(); // Nếu không có discount thì lấy giá gốc

                    // Tính tổng tiền cho item này
                    return discountPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceFee = checkoutService.calculateServiceFee(itemsTotal);
        BigDecimal shippingFee = BigDecimal.valueOf(actualShippingFee);

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
                .shippingCommissionFee(shipperCommissionFee) // ✅ Lưu phí hoa hồng cho shipper
                .notes(request.getNotes())
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        // 11. Tạo order items từ cart items - Lưu SNAPSHOT
        for (CartItem cartItem : itemsToOrder) {
            Dish dish = cartItem.getDish();
            String firstImage = extractFirstImageUrl(dish.getImagesUrls());

            BigDecimal unitPrice = dish.getDiscountPrice() != null
                    ? dish.getDiscountPrice()
                    : cartItem.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .dishId(dish.getId())
                    .dishName(dish.getName())
                    .dishImage(firstImage)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
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
        try {
            orderNotificationService.notifyMerchantNewOrder(savedOrder);
            System.out.println("✅ Sent notification to merchant for order #" + savedOrder.getId());
        } catch (Exception e) {
            System.err.println("❌ Failed to send notification: " + e.getMessage());
        }

        try {
            // Lấy lại cart để đảm bảo có dữ liệu mới nhất
            Cart freshCart = cartRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Giỏ hàng không tồn tại"));

            // Lọc lại items cần xóa dựa trên dishIds
            List<CartItem> itemsToRemove = freshCart.getCartItems().stream()
                    .filter(item -> request.getDishIds().contains(item.getDish().getId()))
                    .collect(Collectors.toList());

            if (!itemsToRemove.isEmpty()) {
                freshCart.getCartItems().removeAll(itemsToRemove);
                cartRepository.save(freshCart);
                log.info("✅ Removed {} items from cart for order #{}", itemsToRemove.size(), savedOrder.getId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to remove items from cart: {}", e.getMessage());
            // Không throw exception vì order đã tạo thành công
        }
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

        // ✅ FIX: Refresh order từ database để lấy dữ liệu mới nhất
        orderRepository.flush();
        order = orderRepository.findById(orderId).orElse(order);

        log.info("📋 Order refreshed - Payment Status: {}, Payment Method: {}, Transaction Ref: {}",
                order.getPaymentStatus(), order.getPaymentMethod(), order.getVnpayTransactionRef());

        OrderStatus oldStatus = order.getStatus();
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();

        // ✅ FIX: Force PAID status nếu có transaction ref (webhook đã process)
        // Lý do: Status có thể bị corrupted thành FAILED, nhưng vnpayTransactionRef là đáng tin cậy hơn
        boolean hasTransactionRef = order.getVnpayTransactionRef() != null &&
                !order.getVnpayTransactionRef().trim().isEmpty();

        if (hasTransactionRef && order.getPaymentStatus() != PaymentStatus.PAID) {
            log.warn("⚠️ Order has transaction ref but status is {}. Forcing PAID...", order.getPaymentStatus());
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("✅ Payment status forced to PAID");
        }

        // Cập nhật trạng thái đơn hàng
        order.updateStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledBy(CancelledBy.CUSTOMER);
        order.setCancelledAt(LocalDateTime.now());

        // Hoàn lại usedCount cho coupon (nếu có)
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            coupon.setUsedCount(coupon.getUsedCount() - 1);
            couponRepository.save(coupon);
        }

        // ✅ Kiểm tra cách khác - dùng vnpayTransactionRef thay vì paymentStatus
        boolean isCardPayment = order.getPaymentMethod() == PaymentMethod.CARD;

        log.info("💳 Refund check - hasTransactionRef: {}, isCardPayment: {}",
                hasTransactionRef, isCardPayment);

        // ✅ Tạo hoàn tiền nếu:
        // 1. Thanh toán bằng CARD
        // 2. Có transaction ref (tức là đã thanh toán)
        if (isCardPayment && hasTransactionRef) {
            log.info("💰 Creating refund request for cancelled order: {}", order.getOrderNumber());

            try {
                RefundRequest refundRequest = refundService.createRefundRequest(order, reason);

                if (refundRequest != null) {
                    log.info("✅ Refund request created successfully - ID: {}", refundRequest.getId());
                } else {
                    log.warn("⚠️ Refund request not created (order not eligible for refund)");
                }
            } catch (Exception e) {
                log.error("❌ Failed to create refund request: ", e);
                // ✅ Không throw exception - chỉ log warning
            }
        } else {
            log.info("ℹ️ No refund needed:");
            if (!isCardPayment) {
                log.info("   - Payment method: {} (not CARD)", order.getPaymentMethod());
            }
            if (!hasTransactionRef) {
                log.info("   - No transaction ref found (payment not completed)");
            }
        }

        Order cancelledOrder = orderRepository.save(order);

        try {
            orderNotificationService.notifyOrderStatusChanged(
                    cancelledOrder,
                    oldStatus,
                    OrderStatus.CANCELLED
            );
            log.info("✅ Sent cancellation notification for order #{}", cancelledOrder.getId());
        } catch (Exception e) {
            log.error("❌ Failed to send notification: ", e.getMessage());
        }

        return mapToOrderResponse(cancelledOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByMerchant(Long merchantId, OrderStatus status) {
        // 1. Nếu có status thì lọc, không thì lấy hết
        List<Order> orders;
        if (status != null) {
            orders = orderRepository.findByMerchantIdAndStatusOrderByDateDesc(merchantId, status.name());
        } else {
            orders = orderRepository.findByMerchantIdWithPriority(merchantId);
        }

        // 2. Sắp xếp đơn mới nhất lên đầu
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long merchantId, Long orderId, OrderStatus newStatus, String cancelReason) {
        // 1. Tìm đơn hàng và kiểm tra quyền sở hữu của merchant
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getMerchant().getId().equals(merchantId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật đơn hàng này");
        }

        // ✅ FIX: Refresh order để lấy dữ liệu mới nhất
        orderRepository.flush();
        order = orderRepository.findById(orderId).orElse(order);

        log.info("📋 Order refreshed for status update - Payment Status: {}, Transaction Ref: {}",
                order.getPaymentStatus(), order.getVnpayTransactionRef());

        // 2. Validate trạng thái
        validateStatusTransition(order.getStatus(), newStatus);

        OrderStatus oldStatus = order.getStatus();
        PaymentStatus oldPaymentStatus = order.getPaymentStatus();

        // 3. Xử lý khi merchant hủy đơn
        if (newStatus == OrderStatus.CANCELLED) {
            if (cancelReason == null || cancelReason.trim().isEmpty()) {
                throw new RuntimeException("Vui lòng cung cấp lý do hủy đơn");
            }

            order.setCancelledBy(CancelledBy.MERCHANT);
            order.setCancellationReason(cancelReason);
            order.setCancelledAt(LocalDateTime.now());

            // Hoàn lại coupon nếu có
            if (order.getCoupon() != null) {
                Coupon coupon = order.getCoupon();
                coupon.setUsedCount(coupon.getUsedCount() - 1);
                couponRepository.save(coupon);
            }

            // ✅ FIX: Force PAID status nếu có transaction ref
            boolean hasTransactionRef = order.getVnpayTransactionRef() != null &&
                    !order.getVnpayTransactionRef().trim().isEmpty();

            if (hasTransactionRef && order.getPaymentStatus() != PaymentStatus.PAID) {
                log.warn("⚠️ [MERCHANT] Order has transaction ref but status is {}. Forcing PAID...",
                        order.getPaymentStatus());
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);
                log.info("✅ Payment status forced to PAID");
            }

            // ✅ Kiểm tra cách khác - dùng vnpayTransactionRef
            boolean isCardPayment = order.getPaymentMethod() == PaymentMethod.CARD;

            log.info("💳 [MERCHANT CANCEL] Refund check - hasTransactionRef: {}, isCardPayment: {}",
                    hasTransactionRef, isCardPayment);

            // ✅ Tạo hoàn tiền nếu:
            // 1. Thanh toán bằng CARD
            // 2. Có transaction ref (tức là đã thanh toán)
            if (isCardPayment && hasTransactionRef) {
                log.info("💰 [MERCHANT CANCEL] Creating refund request for order: {}",
                        order.getOrderNumber());

                try {
                    RefundRequest refundRequest = refundService.createRefundRequest(order, cancelReason);

                    if (refundRequest != null) {
                        log.info("✅ Refund request created successfully - ID: {}", refundRequest.getId());
                    } else {
                        log.warn("⚠️ Refund request not created (order not eligible for refund)");
                    }
                } catch (Exception e) {
                    log.error("❌ Failed to create refund request: ", e);
                    // Không throw exception - chỉ log warning
                }
            } else {
                log.info("ℹ️ No refund needed:");
                if (!isCardPayment) {
                    log.info("   - Payment method: {} (not CARD)", order.getPaymentMethod());
                }
                if (!hasTransactionRef) {
                    log.info("   - No transaction ref found (payment not completed)");
                }
            }
        }

        // 4. Cập nhật trạng thái
        order.updateStatus(newStatus);

        // 5. Lưu và gửi thông báo
        Order savedOrder = orderRepository.save(order);

        try {
            orderNotificationService.notifyOrderStatusChanged(savedOrder, oldStatus, newStatus);
            log.info("✅ Sent notification for order #{}: {} -> {}",
                    savedOrder.getId(), oldStatus, newStatus);
        } catch (Exception e) {
            log.error("❌ Failed to send notification: {}", e.getMessage());
        }

        return mapToOrderResponse(savedOrder);
    }

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

    @Override
    public List<UserResponseDTO> getCustomerByMerchant(Long merchantId) {
        List<User> customers = orderRepository.findDistinctCustomersByMerchantId(merchantId);

        return customers.stream()
                .map(user -> {
                    // Logic tìm địa chỉ mặc định hoặc địa chỉ đầu tiên
                    String defaultAddress = user.getAddresses().stream()
                            .filter(Address::getIsDefault)
                            .findFirst()
                            .map(addr -> buildAddressResponse(addr).buildFullAddress())
                            .orElse(user.getAddresses().isEmpty() ? "Chưa có địa chỉ" : buildAddressResponse(user.getAddresses().get(0)).buildFullAddress());

                    return UserResponseDTO.builder()
                            .id(user.getId())
                            .fullName(user.getFullName())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .dateOfBirth(user.getDateOfBirth())
                            .gender(user.getGender())
                            .shippingAddress(defaultAddress) // Truyền String vào đây
                            .build();
                }) // Thiếu dấu đóng ngoặc nhọn và ngoặc tròn ở đây trong code cũ của bạn
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByCustomerForMerchant(Long UserId, Long merchantId) {
        List<Order> orders = orderRepository.findByUserIdAndMerchantId(UserId, merchantId);

        return orders.stream()
                .sorted(Comparator.comparing(Order::getOrderDate).reversed())//sắp xếp theo ngày mới nhất
                .map(this::mapToOrderResponse)//map order to orderResponse
                .collect(Collectors.toList());//collect to list
    }

    @Override
    public CouponStatisticsResponse getCouponStatistics(Long merchantId, Long couponId) {
        // Lấy danh sách đơn hàng đã sử dụng coupon này tại merchant
        List<Order> orders = orderRepository.findByCouponIdAndMerchantId(couponId, merchantId);
        // Lấy thông tin coupon
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy coupon"));
        // Tính toan thong ke
        BigDecimal totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        //tong so tien da giam cho khach hang
        BigDecimal totalDiscount= orders.stream()
                .map(Order::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // danh sach don hang
        List<OrderResponse> orderResponses = orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
        return CouponStatisticsResponse.builder()
                .couponId(coupon.getId())
                .couponCode(coupon.getCode())
                .totalOrders((long) orders.size())
                .totalRevenue(totalRevenue)
                .totalDiscountGiven(totalDiscount)
                .orders(orderResponses)
                .build();
    }

    @Override
    @Transactional
    public void processSepayPayment(SepayWebhookDTO webhookData) {
        // 1. Trích xuất mã giao dịch (txnRef) từ nội dung chuyển khoản
        // Ví dụ SePay gửi: "THANHTOAN SPY1735622221" -> Cần lấy "SPY1735622221"
        String content = webhookData.getTransferContent();
        if (content == null || !content.contains("SPY")) {
            log.error("Nội dung chuyển khoản không hợp lệ: {}", content);
            return;
        }

        String txnRef = content.substring(content.indexOf("SPY")).trim();
        log.info("Đang xử lý thanh toán cho mã tham chiếu: {}", txnRef);

        // 2. Tìm đơn hàng chờ trong database (Dựa trên vnpayTransactionRef hoặc một trường map tương đương)
        Optional<Order> orderOpt = orderRepository.findByVnpayTransactionRef(txnRef);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();

            // Kiểm tra nếu đơn hàng đã thanh toán rồi thì bỏ qua (Idempotency)
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.warn("Đơn hàng {} đã được thanh toán trước đó.", txnRef);
                return;
            }

            // 3. Kiểm tra số tiền (Quan trọng để tránh gian lận)
            BigDecimal expectedAmount = order.getTotalAmount();
            if (webhookData.getTransferAmount().compareTo(expectedAmount) < 0) {
                log.error("Số tiền thanh toán không đủ! Nhận: {}, Cần: {}",
                        webhookData.getTransferAmount(), expectedAmount);
                return;
            }

            // 4. Cập nhật trạng thái đơn hàng
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setStatus(OrderStatus.CONFIRMED); // Chuyển sang trạng thái đã xác nhận
            orderRepository.save(order);

            log.info("Thanh toán thành công cho đơn hàng: {}", order.getOrderNumber());
        } else {
            log.error("Không tìm thấy đơn hàng với mã tham chiếu: {}", txnRef);
        }
    }

    // Helper: Tách số từ chuỗi
    private Long extractOrderIdFromContent(String content) {
        try {
            // Regex tìm chuỗi số đầu tiên trong nội dung
            // Ví dụ: "DH123" -> lấy 123
            String numberOnly = content.replaceAll("[^0-9]", "");
            if (numberOnly.isEmpty()) return null;
            return Long.parseLong(numberOnly);
        } catch (Exception e) {
            return null;
        }
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
        // ✅ Map shipping address - XỬ LÝ TRƯỜNG HỢP ĐÃ XÓA
        AddressResponse addressResponse = null;
        if (order.getShippingAddress() != null) {
            Address addr = order.getShippingAddress();

            // Kiểm tra xem address có bị soft delete không
            boolean isDeleted = (addr.getDeletedAt() != null);

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

            // ⚠️ Đánh dấu nếu địa chỉ đã bị xóa
            if (isDeleted) {
                addressResponse.setAddressType("Đã xóa");
            }
        } else {
            // ✅ Trường hợp address = null (đã bị xóa hoặc không tồn tại)
            addressResponse = AddressResponse.builder()
                    .contactName("Không rõ")
                    .phone("N/A")
                    .build();
            addressResponse.setFullAddress("Địa chỉ không còn tồn tại");
            addressResponse.setAddressType("Đã xóa");
        }
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
                .customerName(order.getUser().getFullName())
                .customerPhone(order.getUser().getPhone())
                .merchantId(order.getMerchant().getId())
                .merchantName(order.getMerchant().getRestaurantName())
                .merchantAddress(order.getMerchant().getAddress())
                .merchantPhone(order.getMerchant().getPhone())
                .shippingAddress(addressResponse) // ✅ Luôn có giá trị (không bao giờ null)
                .shippingPartnerName(order.getShippingPartner() != null ? order.getShippingPartner().getName() : null)
                .items(items)
                .totalItems(totalItems)
                .itemsTotal(order.getItemsTotal())
                .discountAmount(order.getDiscountAmount())
                .serviceFee(order.getServiceFee())
                .shippingFee(order.getShippingFee())
                .commissionFee(order.getShippingCommissionFee())
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

    // Hàm phụ trợ để kiểm tra logic chuyển trạng thái hợp lệ
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.COMPLETED || current == OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể cập nhật đơn hàng đã kết thúc");
        }
    }

    private AddressResponse buildAddressResponse(Address addr) {
        return AddressResponse.builder()
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
    }
}

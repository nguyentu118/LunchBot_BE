package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;

import java.util.List;

/**
 * Service xử lý đơn hàng
 */
public interface OrderService {
    /**
     * Lấy thông tin checkout (wrapper cho CheckoutService)
     */
    CheckoutResponse getCheckoutInfo(String email);

    /**
     * Áp dụng mã giảm giá (wrapper cho CheckoutService)
     */
    CheckoutResponse applyDiscount(String email, String couponCode);

    OrderResponse createOrder(String email, CheckoutRequest request);

    List<OrderResponse> getOrdersByUser(String email);

    OrderResponse getOrderById(String email, Long orderId);

    OrderResponse cancelOrder(String email, Long orderId, String reason);

    // Lấy danh sách đơn hàng của Merchant (có hỗ trợ lọc theo trạng thái)
    List<OrderResponse> getOrdersByMerchant(Long merchantId, OrderStatus status);

    // Merchant cập nhật trạng thái đơn hàng (Ví dụ: Từ PENDING -> PROCESSING)
    OrderResponse updateOrderStatus(Long merchantId, Long orderId, OrderStatus newStatus, String cancelReason);

    OrderStatisticsResponse getOrderStatisticsByMerchant(Long merchantId);

    // Thêm method này vào interface
    RevenueStatisticsResponse getRevenueStatistics(Long merchantId, String timeRange,Integer week,Integer month, Integer quarter,Integer year, int page, int size);

    Page<OrderResponse> getOrdersByDish(Long merchantId, Long dishId,int page, int size);

    List<UserResponseDTO> getCustomerByMerchant(Long merchantId);

    List<OrderResponse> getOrdersByCustomerForMerchant(Long UserId, Long merchantId);

    CouponStatisticsResponse getCouponStatistics(Long merchantId, Long couponId);
}
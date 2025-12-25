package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;
import vn.codegym.lunchbot_be.dto.response.OrderRevenueDetailDTO;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueReconciliationServiceImpl implements RevenueReconciliationService {

    private final OrderRepository orderRepository;

    // Ngưỡng doanh thu để áp dụng mức chiết khấu thấp hơn
    private static final BigDecimal REVENUE_THRESHOLD = new BigDecimal("200000000"); // 200 triệu

    // Mức chiết khấu sàn
    private static final BigDecimal HIGH_COMMISSION_RATE = new BigDecimal("0.00001"); // 0.001%
    private static final BigDecimal LOW_COMMISSION_RATE = new BigDecimal("0.000005"); // 0.0005%

    @Override
    @Transactional(readOnly = true)
    public MonthlyRevenueResponse getMonthlyReconciliation(Long merchantId, YearMonth yearMonth) {
        // 1. Xác định khoảng thời gian
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 2. Lấy tất cả đơn hàng COMPLETED trong tháng
        List<Order> completedOrders = orderRepository.findCompletedOrdersByDateRange(
                merchantId, startDate, endDate
        );

        // 3. Tính toán từng đơn
        List<OrderRevenueDetailDTO> orderDetails = completedOrders.stream()
                .map(this::calculateOrderRevenue)
                .collect(Collectors.toList());

        // 4. Tính tổng doanh thu (chưa trừ phí)
        BigDecimal totalGrossRevenue = orderDetails.stream()
                .map(OrderRevenueDetailDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Xác định mức chiết khấu áp dụng
        BigDecimal commissionRate = totalGrossRevenue.compareTo(REVENUE_THRESHOLD) >= 0
                ? LOW_COMMISSION_RATE
                : HIGH_COMMISSION_RATE;

        // 6. Tính tổng phí chiết khấu
        BigDecimal totalPlatformFee = BigDecimal.valueOf(orderDetails.size())
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Doanh thu ròng = Doanh thu - Phí chiết khấu
        BigDecimal netRevenue = totalGrossRevenue.subtract(totalPlatformFee);

        // 8. Build response
        return MonthlyRevenueResponse.builder()
                .merchantId(merchantId)
                .yearMonth(yearMonth)
                .totalOrders(orderDetails.size())
                .totalGrossRevenue(totalGrossRevenue)
                .platformCommissionRate(commissionRate.multiply(new BigDecimal("100000"))) // Hiển thị %
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .orderDetails(orderDetails)
                .build();
    }

    /**
     * Tính doanh thu cho 1 đơn hàng
     */
    private OrderRevenueDetailDTO calculateOrderRevenue(Order order) {
        // Doanh thu = itemsTotal - discountAmount
        BigDecimal revenue = order.getItemsTotal()
                .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);

        return OrderRevenueDetailDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderDate(order.getOrderDate())
                .completedAt(order.getCompletedAt())
                .itemsTotal(order.getItemsTotal())
                .discountAmount(order.getDiscountAmount())
                .revenue(revenue)
                .build();
    }
}
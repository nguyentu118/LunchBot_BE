package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.response.*;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.service.RevenueReportService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RevenueReportServiceImpl implements RevenueReportService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;

    private static final BigDecimal REVENUE_THRESHOLD = new BigDecimal("200000000");
    private static final BigDecimal HIGH_COMMISSION_RATE = new BigDecimal("0.00001");
    private static final BigDecimal LOW_COMMISSION_RATE = new BigDecimal("0.000005");

    @Override
    @Transactional(readOnly = true)
    public RevenueReportDTO getDetailedRevenueReport(Long merchantId, YearMonth yearMonth) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

        // Xác định khoảng thời gian
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // Lấy các đơn hoàn thành
        List<Order> completedOrders = orderRepository.findCompletedOrdersByDateRange(
                merchantId, startDate, endDate
        );

        // Lấy các đơn hủy
        List<Order> cancelledOrders = orderRepository.findCancelledOrdersByDateRange(
                merchantId, startDate, endDate
        );

        // Tính doanh thu tổng (chỉ đơn hoàn thành)
        BigDecimal totalGrossRevenue = completedOrders.stream()
                .map(o -> o.getItemsTotal().subtract(
                        o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Xác định mức chiết khấu
        BigDecimal commissionRate = totalGrossRevenue.compareTo(REVENUE_THRESHOLD) >= 0
                ? LOW_COMMISSION_RATE
                : HIGH_COMMISSION_RATE;

        // Tính tổng phí chiết khấu
        BigDecimal totalPlatformFee = totalGrossRevenue
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Doanh thu ròng
        BigDecimal netRevenue = totalGrossRevenue.subtract(totalPlatformFee);

        // Giá trị trung bình đơn
        BigDecimal averageOrderValue = completedOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalGrossRevenue.divide(
                BigDecimal.valueOf(completedOrders.size()), 2, RoundingMode.HALF_UP
        );

        // So sánh với tháng trước
        YearMonth previousMonth = yearMonth.minusMonths(1);
        LocalDateTime prevStartDate = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime prevEndDate = previousMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> previousMonthOrders = orderRepository.findCompletedOrdersByDateRange(
                merchantId, prevStartDate, prevEndDate
        );

        BigDecimal previousMonthRevenue = previousMonthOrders.stream()
                .map(o -> o.getItemsTotal().subtract(
                        o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính sự thay đổi
        BigDecimal revenueChange = totalGrossRevenue.subtract(previousMonthRevenue);
        BigDecimal revenueChangePercent = previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0
                ? revenueChange.divide(previousMonthRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String revenueChangeStatus = revenueChange.compareTo(BigDecimal.ZERO) > 0 ? "UP"
                : revenueChange.compareTo(BigDecimal.ZERO) < 0 ? "DOWN" : "EQUAL";

        // Map DTOs
        List<CompletedOrderDTO> completedOrderDetails = completedOrders.stream()
                .map(o -> CompletedOrderDTO.builder()
                        .orderId(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .orderDate(o.getOrderDate())
                        .completedAt(o.getCompletedAt())
                        .itemsTotal(o.getItemsTotal())
                        .discountAmount(o.getDiscountAmount())
                        .revenue(o.getItemsTotal().subtract(
                                o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO
                        ))
                        .build())
                .collect(Collectors.toList());

        List<CancelledOrderDTO> cancelledOrderDetails = cancelledOrders.stream()
                .map(o -> CancelledOrderDTO.builder()
                        .orderId(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .orderDate(o.getOrderDate())
                        .cancelledAt(o.getCancelledAt())
                        .cancellationReason(o.getCancellationReason())
                        .cancelledBy(o.getCancelledBy() != null ? o.getCancelledBy().name() : "UNKNOWN")
                        .build())
                .collect(Collectors.toList());

        return RevenueReportDTO.builder()
                .merchantId(merchantId)
                .merchantName(merchant.getRestaurantName())
                .period(String.format("%02d/%d", yearMonth.getMonthValue(), yearMonth.getYear()))
                .exportedAt(LocalDateTime.now())
                .totalGrossRevenue(totalGrossRevenue)
                .totalOrders(completedOrders.size() + cancelledOrders.size())
                .completedOrders(completedOrders.size())
                .completedOrderDetails(completedOrderDetails)
                .cancelledOrders(cancelledOrders.size())
                .cancelledOrderDetails(cancelledOrderDetails)
                .averageOrderValue(averageOrderValue)
                .platformCommissionRate(commissionRate)
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .previousMonthRevenue(previousMonthRevenue)
                .revenueChange(revenueChange)
                .revenueChangePercent(revenueChangePercent)
                .revenueChangeStatus(revenueChangeStatus)
                .build();
    }
}
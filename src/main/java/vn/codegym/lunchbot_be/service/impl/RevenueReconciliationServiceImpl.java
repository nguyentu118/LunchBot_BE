package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationRequestCreateDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationReviewDTO;
import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;
import vn.codegym.lunchbot_be.dto.response.OrderRevenueDetailDTO;
import vn.codegym.lunchbot_be.dto.response.ReconciliationRequestResponse;
import vn.codegym.lunchbot_be.dto.response.ReconciliationSummaryResponse;
import vn.codegym.lunchbot_be.model.*;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.ReconciliationRequestRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.ReconciliationNotificationService;
import vn.codegym.lunchbot_be.model.enums.TransactionStatus;
import vn.codegym.lunchbot_be.model.enums.TransactionType;
import vn.codegym.lunchbot_be.repository.*;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RevenueReconciliationServiceImpl implements RevenueReconciliationService {

    private final OrderRepository orderRepository;
    private final ReconciliationRequestRepository reconciliationRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    private final ReconciliationNotificationService reconciliationNotificationService;

    private final TransactionRepository transactionRepository;
    // Ngưỡng doanh thu để áp dụng mức chiết khấu thấp hơn
    private static final BigDecimal REVENUE_THRESHOLD = new BigDecimal("200000000"); // 200 triệu

    private static final BigDecimal HIGH_COMMISSION_RATE = new BigDecimal("0.00001"); // 0.001%
    private static final BigDecimal LOW_COMMISSION_RATE = new BigDecimal("0.000005"); // 0.0005%

    @Override
    @Transactional
    public ReconciliationRequestResponse createReconciliationRequest(Long merchantId, ReconciliationRequestCreateDTO requestDTO) {
        String yearMonthStr = requestDTO.getYearMonth();
        YearMonth yearMonth = YearMonth.parse(yearMonthStr);

        Optional<ReconciliationRequest> existingRequestOpt = reconciliationRepository
                .findByMerchantIdAndYearMonth(merchantId, yearMonthStr);

        ReconciliationRequest request;

        if (existingRequestOpt.isPresent()) {
            ReconciliationRequest existing = existingRequestOpt.get();
            if (existing.getStatus() != ReconciliationStatus.REJECTED) {
                throw new IllegalStateException("Yêu cầu đối soát cho tháng " + yearMonthStr + " đang được xử lý hoặc đã hoàn tất.");
            }

            request = existing;
            request.setStatus(ReconciliationStatus.PENDING);
            request.setMerchantNotes(requestDTO.getMerchantNotes());
            request.setReviewedBy(null);
            request.setReviewedAt(null);
            request.setRejectionReason(null);
            request.setAdminNotes(null);

            MonthlyRevenueResponse newData = getMonthlyReconciliation(merchantId, yearMonth);
            updateRequestFinancialData(request, newData);

        } else {
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

            MonthlyRevenueResponse data = getMonthlyReconciliation(merchantId, yearMonth);

            request = ReconciliationRequest.builder()
                    .merchant(merchant)
                    .yearMonth(yearMonthStr)
                    .status(ReconciliationStatus.PENDING)
                    .merchantNotes(requestDTO.getMerchantNotes())
                    .build();

            updateRequestFinancialData(request, data);
        }

        ReconciliationRequest savedRequest = reconciliationRepository.save(request);

        // ✅ NOTIFY ADMINS about new request
        try {
            reconciliationNotificationService.notifyAdminNewReconciliationRequest(savedRequest);
        } catch (Exception e) {
            log.error("❌ Failed to send admin notification", e);
        }

        return mapToRequestResponse(savedRequest);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse approveRequest(Long requestId, Long adminId) {

        ReconciliationRequest request = reconciliationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đối soát với ID: " + requestId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin user"));

        ReconciliationStatus oldStatus = request.getStatus();

        request.approve(admin);
        Merchant merchant = request.getMerchant();
        BigDecimal netRevenue = request.getNetRevenue(); // Số tiền thực nhận

        BigDecimal balanceBefore = merchant.getCurrentBalance();
        BigDecimal balanceAfter = balanceBefore.add(netRevenue);

        merchant.setCurrentBalance(balanceAfter);
        merchantRepository.save(merchant);

        // 5. TẠO TRANSACTION LOG
        Transaction transaction = Transaction.builder()
                .merchant(merchant)
                .transactionType(TransactionType.MONTHLY_PAYOUT)
                .amount(netRevenue)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .status(TransactionStatus.COMPLETED)
                .transactionDate(LocalDateTime.now())
                .reconciliationRequest(request)
                .notes("Thanh toán doanh thu tháng " + request.getYearMonth())
                .build();

        transactionRepository.save(transaction);

        // 4. Lưu
        ReconciliationRequest saved = reconciliationRepository.save(request);


        // ✅ NOTIFY MERCHANT about approval
        try {
            reconciliationNotificationService.notifyMerchantRequestApproved(saved);
        } catch (Exception e) {
            log.error("❌ Failed to send merchant notification", e);
        }

        return mapToRequestResponse(saved);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse rejectRequest(Long requestId, Long adminId, ReconciliationReviewDTO reviewDTO) {

        ReconciliationRequest request = reconciliationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đối soát với ID: " + requestId));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin user"));

        ReconciliationStatus oldStatus = request.getStatus();

        request.reject(admin, reviewDTO.getRejectionReason());

        if (reviewDTO.getAdminNotes() != null) {
            request.setAdminNotes(reviewDTO.getAdminNotes());
        }

        ReconciliationRequest saved = reconciliationRepository.save(request);


        // ✅ NOTIFY MERCHANT about rejection
        try {
            reconciliationNotificationService.notifyMerchantRequestRejected(saved);
        } catch (Exception e) {
            log.error("❌ Failed to send merchant notification", e);
        }

        return mapToRequestResponse(saved);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse submitRevenueClaim(Long merchantId, ReconciliationClaimDTO claimDTO) {
        String yearMonthStr = claimDTO.getYearMonth();
        YearMonth yearMonth = YearMonth.parse(yearMonthStr);

        Optional<ReconciliationRequest> existingRequestOpt = reconciliationRepository
                .findByMerchantIdAndYearMonth(merchantId, yearMonthStr);

        ReconciliationRequest request;

        if (existingRequestOpt.isPresent()) {
            ReconciliationRequest existing = existingRequestOpt.get();

            if (existing.getStatus() != ReconciliationStatus.REJECTED) {
                throw new IllegalStateException("Yêu cầu cho tháng " + yearMonthStr + " đã tồn tại và đang được xử lý.");
            }

            request = existing;
            request.setStatus(ReconciliationStatus.REPORTED);
            request.setMerchantNotes(claimDTO.getReason());
            request.setReviewedBy(null);
            request.setReviewedAt(null);
            request.setRejectionReason(null);

            MonthlyRevenueResponse newData = getMonthlyReconciliation(merchantId, yearMonth);
            updateRequestFinancialData(request, newData);

        } else {
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

            MonthlyRevenueResponse data = getMonthlyReconciliation(merchantId, yearMonth);

            request = ReconciliationRequest.builder()
                    .merchant(merchant)
                    .yearMonth(yearMonthStr)
                    .status(ReconciliationStatus.REPORTED)
                    .merchantNotes(claimDTO.getReason())
                    .build();

            updateRequestFinancialData(request, data);
        }

        ReconciliationRequest savedRequest = reconciliationRepository.save(request);

        try {
            reconciliationNotificationService.notifyAdminReconciliationClaim(savedRequest);
        } catch (Exception e) {
            log.error("❌ Failed to send admin notification", e);
        }

        return mapToRequestResponse(savedRequest);
    }

    // ... [Keep all other existing methods]

    @Override
    @Transactional(readOnly = true)
    public MonthlyRevenueResponse getMonthlyReconciliation(Long merchantId, YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Order> completedOrders = orderRepository.findCompletedOrdersByDateRange(
                merchantId, startDate, endDate
        );

        List<OrderRevenueDetailDTO> orderDetails = completedOrders.stream()
                .map(this::calculateOrderRevenue)
                .collect(Collectors.toList());

        BigDecimal totalGrossRevenue = orderDetails.stream()
                .map(OrderRevenueDetailDTO::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal commissionRate = totalGrossRevenue.compareTo(REVENUE_THRESHOLD) >= 0
                ? LOW_COMMISSION_RATE
                : HIGH_COMMISSION_RATE;

        BigDecimal totalPlatformFee = totalGrossRevenue
                .multiply(commissionRate)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal netRevenue = totalGrossRevenue.subtract(totalPlatformFee);

        return MonthlyRevenueResponse.builder()
                .merchantId(merchantId)
                .yearMonth(yearMonth)
                .totalOrders(orderDetails.size())
                .totalGrossRevenue(totalGrossRevenue)
                .platformCommissionRate(commissionRate)
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .orderDetails(orderDetails)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReconciliationRequestResponse> getMerchantReconciliationHistory(Long merchantId) {
        List<ReconciliationRequest> requests = reconciliationRepository
                .findByMerchantIdOrderByCreatedAtDesc(merchantId);

        return requests.stream()
                .map(this::mapToRequestResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReconciliationSummaryResponse getReconciliationSummary(Long merchantId) {
        long pending = reconciliationRepository.countByMerchantIdAndStatus(merchantId, ReconciliationStatus.PENDING);
        List<ReconciliationRequest> allRequests = reconciliationRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);

        long total = allRequests.size();
        long approved = allRequests.stream().filter(r -> r.getStatus() == ReconciliationStatus.APPROVED).count();
        long rejected = allRequests.stream().filter(r -> r.getStatus() == ReconciliationStatus.REJECTED).count();

        ReconciliationRequestResponse latest = allRequests.isEmpty() ? null : mapToRequestResponse(allRequests.get(0));

        return ReconciliationSummaryResponse.builder()
                .totalRequests(total)
                .pendingRequests(pending)
                .approvedRequests(approved)
                .rejectedRequests(rejected)
                .latestRequest(latest)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReconciliationRequestResponse> getAllRequestsForAdmin(ReconciliationStatus status, Pageable pageable) {
        Page<ReconciliationRequest> pageResult;

        if (status != null) {
            pageResult = reconciliationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            pageResult = reconciliationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return pageResult.map(this::mapToRequestResponse);
    }

    private ReconciliationRequestResponse mapToRequestResponse(ReconciliationRequest entity) {
        return ReconciliationRequestResponse.builder()
                .id(entity.getId())
                .merchantId(entity.getMerchant().getId())
                .merchantName(entity.getMerchant().getRestaurantName())
                .merchantEmail(entity.getMerchant().getUser().getEmail())
                .merchantPhone(entity.getMerchant().getUser().getPhone())
                .yearMonth(entity.getYearMonth())
                .totalOrders(entity.getTotalOrders())
                .totalGrossRevenue(entity.getTotalGrossRevenue())
                .platformCommissionRate(entity.getPlatformCommissionRate())
                .totalPlatformFee(entity.getTotalPlatformFee())
                .netRevenue(entity.getNetRevenue())
                .status(entity.getStatus())
                .merchantNotes(entity.getMerchantNotes())
                .adminNotes(entity.getAdminNotes())
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .reviewedBy(entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null)
                .reviewedByName(entity.getReviewedBy() != null ? entity.getReviewedBy().getFullName() : null)
                .reviewedAt(entity.getReviewedAt())
                .build();
    }

    private OrderRevenueDetailDTO calculateOrderRevenue(Order order) {
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

    private void updateRequestFinancialData(ReconciliationRequest request, MonthlyRevenueResponse data) {
        request.setTotalOrders(data.getTotalOrders());
        request.setTotalGrossRevenue(data.getTotalGrossRevenue());
        request.setPlatformCommissionRate(data.getPlatformCommissionRate());
        request.setTotalPlatformFee(data.getTotalPlatformFee());
        request.setNetRevenue(data.getNetRevenue());
    }
}
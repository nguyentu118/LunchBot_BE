package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
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
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.ReconciliationRequest;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.ReconciliationRequestRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
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
public class RevenueReconciliationServiceImpl implements RevenueReconciliationService {

    private final OrderRepository orderRepository;
    private final ReconciliationRequestRepository reconciliationRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
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
        BigDecimal totalPlatformFee = totalGrossRevenue
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
                .platformCommissionRate(commissionRate) // Hiển thị %
                .totalPlatformFee(totalPlatformFee)
                .netRevenue(netRevenue)
                .orderDetails(orderDetails)
                .build();
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse createReconciliationRequest(Long merchantId, ReconciliationRequestCreateDTO requestDTO) {
        String yearMonthStr = requestDTO.getYearMonth();
        YearMonth yearMonth = YearMonth.parse(yearMonthStr);

        // 1. Tìm xem đã có request nào chưa
        Optional<ReconciliationRequest> existingRequestOpt = reconciliationRepository
                .findByMerchantIdAndYearMonth(merchantId, yearMonthStr);

        ReconciliationRequest request;

        if (existingRequestOpt.isPresent()) {
            ReconciliationRequest existing = existingRequestOpt.get();
            // Nếu đã có và đang PENDING, REPORTED hoặc APPROVED -> Chặn
            if (existing.getStatus() != ReconciliationStatus.REJECTED) {
                throw new IllegalStateException("Yêu cầu đối soát cho tháng " + yearMonthStr + " đang được xử lý hoặc đã hoàn tất.");
            }

            // Nếu đang REJECTED -> Cho phép GỬI LẠI (Update bản ghi cũ)
            request = existing;
            request.setStatus(ReconciliationStatus.PENDING); // Reset về chờ duyệt
            request.setMerchantNotes(requestDTO.getMerchantNotes());

            // Xóa thông tin duyệt cũ của Admin
            request.setReviewedBy(null);
            request.setReviewedAt(null);
            request.setRejectionReason(null);
            request.setAdminNotes(null); // Reset ghi chú admin cũ (nếu muốn)

            // Cập nhật lại số liệu tài chính (nếu trong thời gian qua số liệu có thay đổi)
            MonthlyRevenueResponse newData = getMonthlyReconciliation(merchantId, yearMonth);
            updateRequestFinancialData(request, newData);

        } else {
            // Chưa có -> Tạo mới hoàn toàn
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
        return mapToRequestResponse(savedRequest);
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
        // Lấy thống kê
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
            // Lọc theo trạng thái (ví dụ: chỉ lấy PENDING)
            pageResult = reconciliationRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            // Lấy tất cả
            pageResult = reconciliationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return pageResult.map(this::mapToRequestResponse);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse approveRequest(Long requestId, Long adminId) {
        // 1. Tìm Request
        ReconciliationRequest request = reconciliationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đối soát với ID: " + requestId));

        // 2. Tìm Admin User
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin user"));

        // 3. Thực hiện logic Approve (Sử dụng method trong Entity)
        request.approve(admin); // Method này set status = APPROVED, reviewedBy, reviewedAt

        // 4. Lưu
        ReconciliationRequest saved = reconciliationRepository.save(request);
        return mapToRequestResponse(saved);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse rejectRequest(Long requestId, Long adminId, ReconciliationReviewDTO reviewDTO) {
        // 1. Tìm Request
        ReconciliationRequest request = reconciliationRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đối soát với ID: " + requestId));

        // 2. Tìm Admin User
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Admin user"));

        // 3. Thực hiện logic Reject (Sử dụng method trong Entity)
        request.reject(admin, reviewDTO.getRejectionReason());

        // Cập nhật thêm Admin Notes nếu có
        if (reviewDTO.getAdminNotes() != null) {
            request.setAdminNotes(reviewDTO.getAdminNotes());
        }

        // 4. Lưu
        ReconciliationRequest saved = reconciliationRepository.save(request);
        return mapToRequestResponse(saved);
    }

    @Override
    @Transactional
    public ReconciliationRequestResponse submitRevenueClaim(Long merchantId, ReconciliationClaimDTO claimDTO) {
        String yearMonthStr = claimDTO.getYearMonth();
        YearMonth yearMonth = YearMonth.parse(yearMonthStr);

        // 1. Tìm request cũ
        Optional<ReconciliationRequest> existingRequestOpt = reconciliationRepository
                .findByMerchantIdAndYearMonth(merchantId, yearMonthStr);

        ReconciliationRequest request;

        if (existingRequestOpt.isPresent()) {
            ReconciliationRequest existing = existingRequestOpt.get();

            // Chỉ cho phép ghi đè nếu trạng thái là REJECTED
            if (existing.getStatus() != ReconciliationStatus.REJECTED) {
                throw new IllegalStateException("Yêu cầu cho tháng " + yearMonthStr + " đã tồn tại và đang được xử lý.");
            }

            // Update lại request bị từ chối thành REPORTED
            request = existing;
            request.setStatus(ReconciliationStatus.REPORTED); // Set trạng thái Khiếu nại
            request.setMerchantNotes(claimDTO.getReason());   // Lý do mới

            // Reset thông tin duyệt cũ
            request.setReviewedBy(null);
            request.setReviewedAt(null);
            request.setRejectionReason(null);

            // Cập nhật lại số liệu (Snapshot mới nhất)
            MonthlyRevenueResponse newData = getMonthlyReconciliation(merchantId, yearMonth);
            updateRequestFinancialData(request, newData);

        } else {
            // Tạo mới (trường hợp chưa từng gửi request nào nhưng muốn báo cáo luôn)
            Merchant merchant = merchantRepository.findById(merchantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Merchant"));

            MonthlyRevenueResponse data = getMonthlyReconciliation(merchantId, yearMonth);

            request = ReconciliationRequest.builder()
                    .merchant(merchant)
                    .yearMonth(yearMonthStr)
                    .status(ReconciliationStatus.REPORTED) // Set trạng thái Khiếu nại
                    .merchantNotes(claimDTO.getReason())
                    .build();

            updateRequestFinancialData(request, data);
        }

        ReconciliationRequest savedRequest = reconciliationRepository.save(request);
        return mapToRequestResponse(savedRequest);
    }


    // Helper: Convert Entity to DTO
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
    private void updateRequestFinancialData(ReconciliationRequest request, MonthlyRevenueResponse data) {
        request.setTotalOrders(data.getTotalOrders());
        request.setTotalGrossRevenue(data.getTotalGrossRevenue());
        request.setPlatformCommissionRate(data.getPlatformCommissionRate());
        request.setTotalPlatformFee(data.getTotalPlatformFee());
        request.setNetRevenue(data.getNetRevenue());
    }
}
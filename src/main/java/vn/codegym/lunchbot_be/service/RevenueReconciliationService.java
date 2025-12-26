package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationRequestCreateDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationReviewDTO;
import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;
import vn.codegym.lunchbot_be.dto.response.ReconciliationRequestResponse;
import vn.codegym.lunchbot_be.dto.response.ReconciliationSummaryResponse;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;

import java.time.YearMonth;
import java.util.List;

public interface RevenueReconciliationService {
    // Method đã có sẵn: Lấy báo cáo doanh thu hàng tháng của Merchant
    MonthlyRevenueResponse getMonthlyReconciliation(Long merchantId, YearMonth yearMonth);

    // Method mới: Tạo yêu cầu đối soát (Gửi cho Admin)
    ReconciliationRequestResponse createReconciliationRequest(Long merchantId, ReconciliationRequestCreateDTO requestDTO);

    // Method mới: Lấy lịch sử yêu cầu đối soát của Merchant
    List<ReconciliationRequestResponse> getMerchantReconciliationHistory(Long merchantId);

    // Method mới: Lấy tổng quan (Summary)
    ReconciliationSummaryResponse getReconciliationSummary(Long merchantId);

    // 1. Lấy danh sách yêu cầu (có phân trang & lọc theo status)
    Page<ReconciliationRequestResponse> getAllRequestsForAdmin(ReconciliationStatus status, Pageable pageable);

    // 2. Duyệt yêu cầu
    ReconciliationRequestResponse approveRequest(Long requestId, Long adminId);

    // 3. Từ chối yêu cầu
    ReconciliationRequestResponse rejectRequest(Long requestId, Long adminId, ReconciliationReviewDTO reviewDTO);
    // Thêm method này vào Interface
    ReconciliationRequestResponse submitRevenueClaim(Long merchantId, ReconciliationClaimDTO claimDTO);


}

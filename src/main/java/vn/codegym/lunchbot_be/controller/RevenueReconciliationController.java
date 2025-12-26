package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationRequestCreateDTO;
import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;
import vn.codegym.lunchbot_be.dto.response.ReconciliationRequestResponse;
import vn.codegym.lunchbot_be.dto.response.ReconciliationSummaryResponse;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants/revenue-reconciliation")
@RequiredArgsConstructor
public class RevenueReconciliationController {

    private final RevenueReconciliationService revenueReconciliationService;
    private final MerchantServiceImpl merchantService;

    /**
     * GET /api/merchants/revenue-reconciliation/monthly?yearMonth=2024-12
     */
    @GetMapping("/monthly")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getMonthlyReconciliation(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) String yearMonth
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            // Nếu không truyền yearMonth, lấy tháng hiện tại
            YearMonth targetMonth = yearMonth != null
                    ? YearMonth.parse(yearMonth)
                    : YearMonth.now();

            MonthlyRevenueResponse response = revenueReconciliationService
                    .getMonthlyReconciliation(merchantId, targetMonth);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi đối soát doanh thu: " + e.getMessage()));
        }
    }

    // 2. Gửi yêu cầu đối soát (Submit) - MỚI
    @PostMapping("/request")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> createReconciliationRequest(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReconciliationRequestCreateDTO requestDTO
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationRequestResponse response = revenueReconciliationService
                    .createReconciliationRequest(merchantId, requestDTO);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            // Lỗi nghiệp vụ (ví dụ: đã tồn tại request)
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi tạo yêu cầu đối soát: " + e.getMessage()));
        }
    }

    // 3. Xem lịch sử/danh sách yêu cầu - MỚI
    @GetMapping("/history")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getHistory(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            List<ReconciliationRequestResponse> history = revenueReconciliationService
                    .getMerchantReconciliationHistory(merchantId);

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 4. Xem tổng quan (Summary) - MỚI (Tùy chọn, tốt cho Dashboard)
    @GetMapping("/summary")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getSummary(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationSummaryResponse summary = revenueReconciliationService
                    .getReconciliationSummary(merchantId);

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    //  Báo cáo sai sót (Claim)
    @PostMapping("/claim")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> submitClaim(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ReconciliationClaimDTO claimDTO
    ) {
        try {
            Long userId = userDetails.getId();
            Long merchantId = merchantService.getMerchantIdByUserId(userId);

            ReconciliationRequestResponse response = revenueReconciliationService
                    .submitRevenueClaim(merchantId, claimDTO);

            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi gửi báo cáo: " + e.getMessage()));
        }
    }
}
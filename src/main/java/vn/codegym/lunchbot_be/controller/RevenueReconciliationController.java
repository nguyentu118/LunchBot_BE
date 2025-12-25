package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.response.MonthlyRevenueResponse;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.time.YearMonth;
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
}
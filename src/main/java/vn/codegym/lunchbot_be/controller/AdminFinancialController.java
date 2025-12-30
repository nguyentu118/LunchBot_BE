package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.model.enums.WithdrawalStatus;
import vn.codegym.lunchbot_be.service.FinancialService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/financial")
@RequiredArgsConstructor
public class AdminFinancialController {

    private final FinancialService financialService;

    // 1. Lấy danh sách yêu cầu rút tiền (Thường lọc PENDING để xử lý)
    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getWithdrawals(@RequestParam(required = false) WithdrawalStatus status) {
        return ResponseEntity.ok(financialService.getWithdrawalRequestsByStatus(status));
    }

    // 2. Duyệt yêu cầu (Chuyển tiền thành công)
    @PutMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveWithdrawal(@PathVariable Long id) {
        try {
            financialService.approveWithdrawal(id);
            return ResponseEntity.ok(Map.of("message", "Đã duyệt yêu cầu rút tiền."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. Từ chối yêu cầu (Hoàn tiền về ví)
    @PutMapping("/withdrawals/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectWithdrawal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.get("reason");
            financialService.rejectWithdrawal(id, reason);
            return ResponseEntity.ok(Map.of("message", "Đã từ chối và hoàn tiền về ví Merchant."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.WithdrawalCreateDTO;
import vn.codegym.lunchbot_be.service.FinancialService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/merchants/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialService financialService;
    private final MerchantServiceImpl merchantService;

    // API Rút tiền
    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> requestWithdrawal(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WithdrawalCreateDTO requestDTO
    ) {
        try {
            Long merchantId = merchantService.getMerchantIdByUserId(userDetails.getId());
            financialService.createWithdrawalRequest(merchantId, requestDTO);
            return ResponseEntity.ok(Map.of("message", "Yêu cầu rút tiền thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // API Thanh lý hợp đồng
    @PostMapping("/liquidate")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> liquidateContract(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WithdrawalCreateDTO bankInfoDTO
    ) {
        try {
            Long merchantId = merchantService.getMerchantIdByUserId(userDetails.getId());
            financialService.liquidateContract(merchantId, bankInfoDTO);
            return ResponseEntity.ok(Map.of("message", "Đã gửi yêu cầu thanh lý. Tài khoản đã bị khóa để xử lý."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // API Lịch sử rút tiền
    @GetMapping("/history")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long merchantId = merchantService.getMerchantIdByUserId(userDetails.getId());
        return ResponseEntity.ok(financialService.getMerchantWithdrawalHistory(merchantId));
    }
}
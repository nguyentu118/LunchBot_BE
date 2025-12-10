package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.MerchantApprovalRequest;
import vn.codegym.lunchbot_be.dto.request.MerchantLockRequest;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantListResponse;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantResponse;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
import vn.codegym.lunchbot_be.service.AdminMerchantService;

@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    /**
     * API duyệt/từ chối merchant - TASK 25
     */
    @PutMapping("/{merchantId}/approval")
    public ResponseEntity<AdminMerchantResponse> approveMerchant(
            @PathVariable("merchantId") Long merchantId,
            @Valid @RequestBody MerchantApprovalRequest request) {

        AdminMerchantResponse response = adminMerchantService.approveMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }
    //  API KHÓA/MỞ KHÓA MERCHANT - TASK 28
    @PutMapping("/{merchantId}/lock")
    public ResponseEntity<AdminMerchantResponse> lockUnlockMerchant(
            @PathVariable("merchantId") Long merchantId,
            @Valid @RequestBody MerchantLockRequest request) {

        AdminMerchantResponse response = adminMerchantService.lockUnlockMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }

    //  API LẤY TẤT CẢ MERCHANT (TASK 26)
    @GetMapping
    public ResponseEntity<Page<AdminMerchantListResponse>> getAllMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminMerchantService.getAllMerchants(pageable));
    }
    
    //  API LỌC THEO STATUS (APPROVED, PENDING...)
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AdminMerchantListResponse>> getByStatus(
            @PathVariable("status") MerchantStatus status,  // ← Đổi từ @RequestParam sang @PathVariable
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminMerchantService.getMerchantsByStatus(status, pageable));
    }
    
    //  API SEARCH
    @GetMapping("/search")
    public ResponseEntity<Page<AdminMerchantListResponse>> searchMerchants(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(adminMerchantService.searchMerchants(keyword, pageable));
    }
    /**
     * API đưa merchant bị từ chối quay lại trạng thái chờ duyệt - TASK 26 (Bổ sung)
     */
    @PutMapping("/{merchantId}/re-process")
    public ResponseEntity<AdminMerchantResponse> reProcessMerchant(
            @PathVariable("merchantId") Long merchantId,
            @Valid @RequestBody MerchantApprovalRequest request) {

        AdminMerchantResponse response = adminMerchantService.reProcessMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }
    //  BỔ SUNG: API LẤY CHI TIẾT MERCHANT - TASK 27
    @GetMapping("/{merchantId}")
    public ResponseEntity<AdminMerchantResponse> getMerchantDetails(
            @PathVariable("merchantId") Long merchantId) {

        AdminMerchantResponse response = adminMerchantService.getMerchantDetails(merchantId);
        return ResponseEntity.ok(response);
    }
}
// controller/AdminMerchantController.java
package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
            @PathVariable Long merchantId,
            @Valid @RequestBody MerchantApprovalRequest request) {

        AdminMerchantResponse response = adminMerchantService.approveMerchant(merchantId, request);
        return ResponseEntity.ok(response);
    }

    // Các API khác cho các task sau sẽ thêm ở đây...
}

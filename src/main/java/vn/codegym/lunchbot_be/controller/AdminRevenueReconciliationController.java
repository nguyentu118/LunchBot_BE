package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationReviewDTO;
import vn.codegym.lunchbot_be.dto.response.ReconciliationRequestResponse;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reconciliation")
@RequiredArgsConstructor
public class AdminRevenueReconciliationController {

    private final RevenueReconciliationService reconciliationService;


    // 1. Lấy danh sách yêu cầu (Filter + Pagination)
    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRequests(
            @RequestParam(required = false) ReconciliationStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable
    ) {
        Page<ReconciliationRequestResponse> result = reconciliationService
                .getAllRequestsForAdmin(status, pageable);
        return ResponseEntity.ok(result);
    }
    // 2. Duyệt yêu cầu (Approve)
    @PutMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,// ID của yêu cầu đối soát
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long adminId = userDetails.getId();// Lấy ID admin từ UserDetails
            ReconciliationRequestResponse response = reconciliationService.approveRequest(id, adminId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // 3. Từ chối yêu cầu (Reject)
    @PutMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody ReconciliationReviewDTO reviewDTO,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        try {
            Long adminId = userDetails.getId();
            ReconciliationRequestResponse response = reconciliationService.rejectRequest(id, adminId, reviewDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

}
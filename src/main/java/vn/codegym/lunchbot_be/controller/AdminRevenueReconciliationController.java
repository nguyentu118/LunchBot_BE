package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.ReconciliationClaimDTO;
import vn.codegym.lunchbot_be.dto.request.ReconciliationReviewDTO;
import vn.codegym.lunchbot_be.dto.response.ReconciliationRequestResponse;
import vn.codegym.lunchbot_be.model.ReconciliationRequest;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;
import vn.codegym.lunchbot_be.repository.ReconciliationRequestRepository;
import vn.codegym.lunchbot_be.service.RevenueReconciliationService;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reconciliation")
@RequiredArgsConstructor
public class AdminRevenueReconciliationController {

    private final RevenueReconciliationService reconciliationService;
    private final ReconciliationRequestRepository reconciliationRepository;


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
    /**
     * GET /api/admin/reconciliation/{requestId}/claim-file/download
     * Tải file báo cáo sai sót
     */
    @GetMapping("/{requestId}/claim-file/download")
    public ResponseEntity<?> downloadClaimFile(
            @PathVariable Long requestId
    ) {
        try {
            // Lấy thông tin request
            ReconciliationRequest request = reconciliationRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đối soát"));

            Long merchantId = request.getMerchant().getId();
            String uploadDir = "claims/" + merchantId;

            // Tìm file (file được lưu với pattern: claim_[requestId]_[timestamp].xlsx)
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                return ResponseEntity.notFound().build();
            }

            // Tìm file theo requestId
            Path[] files = Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().startsWith("claim_" + requestId + "_"))
                    .toArray(Path[]::new);

            if (files.length == 0) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = files[0]; // Lấy file đầu tiên (thường chỉ có 1)
            byte[] fileContent = Files.readAllBytes(filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "BaoCao_DoanhThu_" + request.getYearMonth() + ".xlsx");
            headers.setContentLength(fileContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi khi đọc file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }


}
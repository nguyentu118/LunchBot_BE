package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.ConfirmRefundRequest;
import vn.codegym.lunchbot_be.dto.response.RefundResponse;
import vn.codegym.lunchbot_be.service.impl.RefundServiceImpl;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * API quản lý hoàn tiền (Admin only)
 */
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('MERCHANT')")
public class RefundController {

    private final RefundServiceImpl refundService;

    /**
     * Lấy danh sách yêu cầu hoàn tiền chờ xử lý
     * GET /api/admin/refunds/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingRefunds() {
        try {
            log.info("📋 Admin fetching pending refunds");

            List<RefundResponse> refunds = refundService.getPendingRefunds();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds,
                    "count", refunds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Error getting pending refunds: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Lấy tất cả yêu cầu hoàn tiền
     * GET /api/admin/refunds
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllRefunds() {
        try {
            log.info("📋 Admin fetching all refunds");

            List<RefundResponse> refunds = refundService.getAllRefunds();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refunds,
                    "count", refunds.size()
            ));

        } catch (Exception e) {
            log.error("❌ Error getting all refunds: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Lấy chi tiết yêu cầu hoàn tiền
     * GET /api/admin/refunds/{refundId}
     */
    @GetMapping("/{refundId}")
    public ResponseEntity<Map<String, Object>> getRefundDetail(@PathVariable Long refundId) {
        try {
            log.info("🔍 Admin fetching refund detail: {}", refundId);

            RefundResponse refund = refundService.getRefundById(refundId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", refund
            ));

        } catch (Exception e) {
            log.error("❌ Error getting refund detail: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * ✅ MỚI: Chuyển sang PROCESSING
     * POST /api/admin/refunds/{refundId}/processing
     * Body: { "notes": "Đang kiểm tra thông tin..." }
     */
    @PostMapping("/{refundId}/processing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> markAsProcessing(
            @PathVariable Long refundId,
            @RequestBody(required = false) Map<String, String> request,
            Principal principal
    ) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin";
            String notes = request != null ? request.get("notes") : null;

            log.info("🔄 Admin {} marking refund {} as PROCESSING", adminEmail, refundId);

            RefundResponse result = refundService.markAsProcessing(refundId, adminEmail, notes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã chuyển sang trạng thái ĐANG XỬ LÝ",
                    "data", result
            ));
        } catch (Exception e) {
            log.error("❌ Error marking as processing: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    /**
     * Admin xác nhận đã hoàn tiền thủ công
     * POST /api/admin/refunds/{refundId}/confirm
     *
     * Body: {
     *   "refundTransactionRef": "REF123456",
     *   "notes": "Đã chuyển khoản về tài khoản ...1234"
     * }
     */
    @PostMapping("/{refundId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> confirmRefund(
            @PathVariable Long refundId,
            @RequestBody ConfirmRefundRequest request,
            Principal principal
    ) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin";

            log.info("✅ Admin {} confirming refund: {}", adminEmail, refundId);

            refundService.confirmRefund(
                    refundId,
                    adminEmail,
                    request.getRefundTransactionRef(),
                    request.getNotes()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xác nhận hoàn tiền thành công"
            ));

        } catch (Exception e) {
            log.error("❌ Error confirming refund: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Đánh dấu hoàn tiền thất bại
     * POST /api/admin/refunds/{refundId}/fail
     *
     * Body: {
     *   "reason": "Sai thông tin tài khoản"
     * }
     */
    @PostMapping("/{refundId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> markRefundFailed(
            @PathVariable Long refundId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin";
            String reason = request.get("reason");

            log.warn("⚠️ Admin {} marking refund {} as failed", adminEmail, refundId);

            refundService.markRefundFailed(refundId, adminEmail, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã đánh dấu hoàn tiền thất bại"
            ));

        } catch (Exception e) {
            log.error("❌ Error marking refund as failed: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Hủy yêu cầu hoàn tiền
     * POST /api/admin/refunds/{refundId}/cancel
     *
     * Body: {
     *   "reason": "Khách hàng yêu cầu hủy"
     * }
     */
    @PostMapping("/{refundId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelRefund(
            @PathVariable Long refundId,
            @RequestBody Map<String, String> request,
            Principal principal
    ) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin";
            String reason = request.get("reason");

            log.info("🚫 Admin {} cancelling refund {}", adminEmail, refundId);

            refundService.cancelRefund(refundId, adminEmail, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã hủy yêu cầu hoàn tiền"
            ));

        } catch (Exception e) {
            log.error("❌ Error cancelling refund: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
    /**
     * ✅ MỚI: Retry refund từ FAILED
     * POST /api/admin/refunds/{refundId}/retry
     */
    @PostMapping("/{refundId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryRefund(
            @PathVariable Long refundId,
            Principal principal
    ) {
        try {
            String adminEmail = principal != null ? principal.getName() : "admin";

            log.info("🔄 Admin {} retrying refund {}", adminEmail, refundId);

            RefundResponse result = refundService.retryRefund(refundId, adminEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã reset refund về PENDING để thử lại",
                    "data", result
            ));
        } catch (Exception e) {
            log.error("❌ Error retrying refund: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
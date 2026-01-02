package vn.codegym.lunchbot_be.service.impl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.response.RefundResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.RefundRequest;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.RefundRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundServiceImpl {
    private final RefundRequestRepository refundRequestRepository;
    private final OrderRepository orderRepository;

    /**
     * Tạo yêu cầu hoàn tiền khi đơn hàng bị hủy
     * ✅ FIX: Chỉ tạo hoàn tiền nếu đơn hàng đã thanh toán online
     */
    @Transactional
    public RefundRequest createRefundRequest(Order order, String reason) {
        try {
            log.info("💰 Creating refund request for order: {}", order.getOrderNumber());
            log.info("📊 Order Details - ID: {}, PaymentStatus: {}, PaymentMethod: {}",
                    order.getId(), order.getPaymentStatus(), order.getPaymentMethod());

            // ✅ FIX: Kiểm tra thanh toán status
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                log.warn("⚠️ Order {} is not paid (Status: {}), no refund needed",
                        order.getOrderNumber(), order.getPaymentStatus());
                log.warn("💡 Expected PAID but got: {}", order.getPaymentStatus());
                return null; // ← Trả về null thay vì throw exception
            }

            // ✅ FIX: Chỉ hoàn tiền nếu thanh toán bằng CARD (VNPay/SePay)
            if (order.getPaymentMethod() != PaymentMethod.CARD) {
                log.info("⚠️ Order {} paid by {}, no refund needed",
                        order.getOrderNumber(), order.getPaymentMethod());
                log.info("💡 Refund only applies to CARD payments, this is: {}",
                        order.getPaymentMethod());
                return null; // COD không cần hoàn tiền
            }

            // Kiểm tra đã có yêu cầu hoàn tiền chưa
            if (refundRequestRepository.existsByOrderId(order.getId())) {
                log.warn("⚠️ Refund request already exists for order: {}", order.getId());
                return refundRequestRepository.findByOrderId(order.getId()).orElse(null);
            }

            // Tạo yêu cầu hoàn tiền
            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setOrder(order);
            refundRequest.setRefundAmount(order.getTotalAmount());
            refundRequest.setRefundReason(reason);
            refundRequest.setTransactionRef(order.getVnpayTransactionRef());
            refundRequest.setRefundStatus(RefundStatus.PENDING);

            // ✅ Lấy thông tin tài khoản ngân hàng từ user
            User user = order.getUser();
            if (user != null) {
                refundRequest.setCustomerBankAccount(user.getBankAccountNumber());
                refundRequest.setCustomerBankName(user.getBankName());
                refundRequest.setCustomerAccountName(user.getBankAccountName());

                log.info("🏦 Bank info: {} - {} - {}",
                        user.getBankName(),
                        user.getBankAccountNumber(),
                        user.getBankAccountName());
            } else {
                log.warn("⚠️ User not found for order: {}", order.getId());
            }

            refundRequest = refundRequestRepository.save(refundRequest);

            // ✅ Cập nhật trạng thái thanh toán đơn hàng
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            orderRepository.save(order);

            log.info("✅ Refund request created successfully: ID={}", refundRequest.getId());

            return refundRequest;

        } catch (Exception e) {
            log.error("❌ Error creating refund request: ", e);
            throw new RuntimeException("Không thể tạo yêu cầu hoàn tiền: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách yêu cầu hoàn tiền chờ xử lý
     */
    public List<RefundResponse> getPendingRefunds() {
        List<RefundRequest> refunds = refundRequestRepository
                .findByRefundStatusOrderByCreatedAtDesc(RefundStatus.PENDING);
        return refunds.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    @Transactional
    public RefundResponse markAsProcessing(Long refundId, String adminEmail, String notes) {
        log.info("🔄 Marking refund {} as PROCESSING", refundId);

        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getRefundStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể chuyển sang PROCESSING từ trạng thái PENDING");
        }

        refund.setRefundStatus(RefundStatus.PROCESSING);
        refund.setProcessedBy(adminEmail);
        if (notes != null && !notes.trim().isEmpty()) {
            refund.setNotes(notes);
        }

        refundRequestRepository.save(refund);
        log.info("✅ Refund marked as PROCESSING");

        return convertToResponse(refund);
    }

    /**
     * Lấy tất cả yêu cầu hoàn tiền
     */
    public List<RefundResponse> getAllRefunds() {
        List<RefundRequest> refunds = refundRequestRepository.findAllByOrderByCreatedAtDesc();
        return refunds.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết yêu cầu hoàn tiền
     */
    public RefundResponse getRefundById(Long refundId) {
        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
        return convertToResponse(refund);
    }

    /**
     * Admin xác nhận đã hoàn tiền thủ công
     */
    @Transactional
    public void confirmRefund(Long refundId, String adminEmail, String refundTransactionRef, String notes) {
        try {
            log.info("✅ Confirming refund: ID={}", refundId);

            RefundRequest refund = refundRequestRepository.findById(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

            // Cập nhật trạng thái
            refund.setRefundStatus(RefundStatus.COMPLETED);
            refund.setProcessedAt(LocalDateTime.now());
            refund.setProcessedBy(adminEmail);
            refund.setRefundTransactionRef(refundTransactionRef);
            refund.setNotes(notes);

            refundRequestRepository.save(refund);

            // Cập nhật trạng thái đơn hàng
            Order order = refund.getOrder();
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);

            log.info("✅ Refund completed successfully for order: {}", order.getOrderNumber());

        } catch (Exception e) {
            log.error("❌ Error confirming refund: ", e);
            throw new RuntimeException("Không thể xác nhận hoàn tiền: " + e.getMessage());
        }
    }

    /**
     * Đánh dấu hoàn tiền thất bại
     */
    @Transactional
    public void markRefundFailed(Long refundId, String adminEmail, String reason) {
        try {
            RefundRequest refund = refundRequestRepository.findById(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

            refund.setRefundStatus(RefundStatus.FAILED);
            refund.setProcessedAt(LocalDateTime.now());
            refund.setProcessedBy(adminEmail);
            refund.setNotes(reason);

            refundRequestRepository.save(refund);

            log.warn("⚠️ Refund marked as failed: {}", reason);

        } catch (Exception e) {
            log.error("❌ Error marking refund as failed: ", e);
            throw new RuntimeException("Không thể đánh dấu thất bại: " + e.getMessage());
        }
    }

    /**
     * Hủy yêu cầu hoàn tiền
     */
    @Transactional
    public void cancelRefund(Long refundId, String adminEmail, String reason) {
        try {
            RefundRequest refund = refundRequestRepository.findById(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

            refund.setRefundStatus(RefundStatus.CANCELLED);
            refund.setProcessedAt(LocalDateTime.now());
            refund.setProcessedBy(adminEmail);
            refund.setNotes(reason);

            refundRequestRepository.save(refund);

            // Khôi phục trạng thái thanh toán đơn hàng về PAID
            Order order = refund.getOrder();
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            log.info("🚫 Refund cancelled: {}", reason);

        } catch (Exception e) {
            log.error("❌ Error cancelling refund: ", e);
            throw new RuntimeException("Không thể hủy yêu cầu hoàn tiền: " + e.getMessage());
        }
    }

    @Transactional
    public RefundResponse retryRefund(Long refundId, String adminEmail) {
        log.info("🔄 Retrying refund {}", refundId);

        RefundRequest refund = refundRequestRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));

        if (refund.getRefundStatus() != RefundStatus.FAILED) {
            throw new IllegalStateException("Chỉ có thể retry refund từ trạng thái FAILED");
        }

        // Reset về PENDING
        refund.setRefundStatus(RefundStatus.PENDING);
        refund.setProcessedBy(adminEmail);
        refund.setProcessedAt(null);
        refund.setNotes("Retry refund request");

        refundRequestRepository.save(refund);

        log.info("✅ Refund reset to PENDING for retry");

        return convertToResponse(refund);
    }
    /**
     * ✅ THÊM: Lấy refunds theo status
     */
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsByStatus(RefundStatus status) {
        List<RefundRequest> refunds = refundRequestRepository
                .findByRefundStatusOrderByCreatedAtDesc(status);
        return refunds.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    /**
     * Convert Entity sang DTO Response
     */
    private RefundResponse convertToResponse(RefundRequest refund) {
        Order order = refund.getOrder();

        return RefundResponse.builder()
                .id(refund.getId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : "N/A")
                .customerName(order.getUser() != null ? order.getUser().getFullName() : "N/A")
                .refundAmount(refund.getRefundAmount())
                .customerBankAccount(refund.getCustomerBankAccount())
                .customerBankName(refund.getCustomerBankName())
                .customerAccountName(refund.getCustomerAccountName())
                .refundStatus(refund.getRefundStatus())
                .refundReason(refund.getRefundReason())
                .transactionRef(refund.getTransactionRef())
                .refundTransactionRef(refund.getRefundTransactionRef())
                .createdAt(refund.getCreatedAt())
                .processedAt(refund.getProcessedAt())
                .processedBy(refund.getProcessedBy())
                .notes(refund.getNotes())
                .build();
    }
}
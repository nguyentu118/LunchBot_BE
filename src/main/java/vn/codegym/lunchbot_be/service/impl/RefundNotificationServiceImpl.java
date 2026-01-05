package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.RefundRequest;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.NotificationService;
import vn.codegym.lunchbot_be.service.RefundNotificationService;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static vn.codegym.lunchbot_be.model.enums.UserRole.ADMIN;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundNotificationServiceImpl implements RefundNotificationService {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    @Transactional
    public void notifyAdminNewRefundRequest(RefundRequest refundRequest) {
        Order order = refundRequest.getOrder();
        String title = "🔔 Yêu cầu hoàn tiền mới #" + refundRequest.getId();
        String content = String.format(
                "Đơn hàng #%s - Khách hàng %s yêu cầu hoàn tiền %s. Lý do: %s",
                order.getOrderNumber(),
                order.getUser().getFullName(),
                formatCurrency(refundRequest.getRefundAmount()),
                refundRequest.getRefundReason()
        );

        sendNotificationToAllAdmins(refundRequest, title, content, NotificationType.REFUND_REQUESTED);
        log.info("✅ Sent refund notification to all admins for refund #{}", refundRequest.getId());
    }

    @Override
    @Transactional
    public void notifyUserRefundProcessing(RefundRequest refundRequest) {
        String title = "⏳ Yêu cầu hoàn tiền đang được xử lý";
        String content = String.format(
                "Yêu cầu hoàn tiền cho đơn hàng #%s đang được xử lý. Số tiền: %s. " +
                        "Chúng tôi sẽ thông báo khi hoàn tiền hoàn tất.",
                refundRequest.getOrder().getOrderNumber(),
                formatCurrency(refundRequest.getRefundAmount())
        );

        sendNotificationToUser(refundRequest, title, content, NotificationType.REFUND_PROCESSING);
    }

    @Override
    @Transactional
    public void notifyUserRefundCompleted(RefundRequest refundRequest) {
        String title = "✅ Hoàn tiền thành công";
        String content = String.format(
                "Yêu cầu hoàn tiền cho đơn hàng #%s đã hoàn tất. Số tiền %s sẽ được chuyển về tài khoản %s - %s trong 1-3 ngày làm việc.",
                refundRequest.getOrder().getOrderNumber(),
                formatCurrency(refundRequest.getRefundAmount()),
                refundRequest.getCustomerBankName(),
                refundRequest.getCustomerBankAccount()
        );

        sendNotificationToUser(refundRequest, title, content, NotificationType.REFUND_COMPLETED);
    }

    @Override
    @Transactional
    public void notifyUserRefundFailed(RefundRequest refundRequest, String reason) {
        String title = "❌ Hoàn tiền thất bại";
        String content = String.format(
                "Rất tiếc, hoàn tiền cho đơn hàng #%s không thành công. Lý do: %s. " +
                        "Vui lòng liên hệ bộ phận chăm sóc khách hàng để được hỗ trợ.",
                refundRequest.getOrder().getOrderNumber(),
                reason != null ? reason : "Không xác định"
        );

        sendNotificationToUser(refundRequest, title, content, NotificationType.REFUND_FAILED);
    }

    @Override
    @Transactional
    public void notifyUserRefundCancelled(RefundRequest refundRequest, String reason) {
        String title = "🚫 Yêu cầu hoàn tiền đã bị hủy";
        String content = String.format(
                "Yêu cầu hoàn tiền cho đơn hàng #%s đã bị hủy. Lý do: %s",
                refundRequest.getOrder().getOrderNumber(),
                reason != null ? reason : "Không có lý do cụ thể"
        );

        sendNotificationToUser(refundRequest, title, content, NotificationType.REFUND_CANCELLED);
    }

    @Override
    @Transactional
    public void notifyRefundStatusChanged(RefundRequest refundRequest, RefundStatus oldStatus, RefundStatus newStatus) {
        log.info("💰 Refund #{} status changed from {} to {}", refundRequest.getId(), oldStatus, newStatus);

        switch (newStatus) {
            case PENDING:
                // Thông báo cho admin khi có yêu cầu mới
                notifyAdminNewRefundRequest(refundRequest);
                break;

            case PROCESSING:
                // Thông báo cho user khi admin bắt đầu xử lý
                notifyUserRefundProcessing(refundRequest);
                break;

            case COMPLETED:
                // Thông báo cho user khi hoàn tiền thành công
                notifyUserRefundCompleted(refundRequest);

                // Thông báo cho admin biết đã hoàn thành
                notifyAdminRefundCompleted(refundRequest);
                break;

            case FAILED:
                // Thông báo cho user khi hoàn tiền thất bại
                notifyUserRefundFailed(refundRequest, refundRequest.getNotes());

                // Thông báo cho admin biết thất bại
                notifyAdminRefundFailed(refundRequest);
                break;

            case CANCELLED:
                // Thông báo cho user khi yêu cầu bị hủy
                notifyUserRefundCancelled(refundRequest, refundRequest.getNotes());
                break;

            default:
                log.warn("⚠️ Unhandled refund status: {}", newStatus);
        }
    }

    /**
     * Thông báo cho admin khi hoàn tiền hoàn tất
     */
    private void notifyAdminRefundCompleted(RefundRequest refundRequest) {
        String title = "✅ Hoàn tiền hoàn tất #" + refundRequest.getId();
        String content = String.format(
                "Đơn hàng #%s - Đã hoàn tiền %s thành công cho khách hàng %s.",
                refundRequest.getOrder().getOrderNumber(),
                formatCurrency(refundRequest.getRefundAmount()),
                refundRequest.getOrder().getUser().getFullName()
        );

        sendNotificationToAllAdmins(refundRequest, title, content, NotificationType.REFUND_COMPLETED);
    }

    /**
     * Thông báo cho admin khi hoàn tiền thất bại
     */
    private void notifyAdminRefundFailed(RefundRequest refundRequest) {
        String title = "❌ Hoàn tiền thất bại #" + refundRequest.getId();
        String content = String.format(
                "Đơn hàng #%s - Hoàn tiền thất bại. Lý do: %s",
                refundRequest.getOrder().getOrderNumber(),
                refundRequest.getNotes() != null ? refundRequest.getNotes() : "Không xác định"
        );

        sendNotificationToAllAdmins(refundRequest, title, content, NotificationType.REFUND_FAILED);
    }

    /**
     * Gửi thông báo đến user
     */
    private void sendNotificationToUser(RefundRequest refundRequest, String title, String content, NotificationType type) {
        User user = refundRequest.getOrder().getUser();

        Notification notification = Notification.builder()
                .user(user)
                .merchant(refundRequest.getOrder().getMerchant())
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .build();

        // Lưu vào database
        notification = notificationRepository.save(notification);

        // Gửi qua WebSocket
        notificationService.sendPrivateNotification(user.getEmail(), notification);

        log.info("📧 Sent refund notification to user {}: {}", user.getEmail(), title);
    }

    /**
     * Gửi thông báo đến tất cả admin
     */
    private void sendNotificationToAllAdmins(RefundRequest refundRequest, String title, String content, NotificationType type) {
        // Lấy danh sách tất cả admin
        List<User> admins = userRepository.findByRole(ADMIN);

        if (admins.isEmpty()) {
            log.warn("⚠️ No admin users found to send refund notification");
            return;
        }

        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .user(admin)
                    .merchant(refundRequest.getOrder().getMerchant())
                    .title(title)
                    .content(content)
                    .type(type)
                    .isRead(false)
                    .build();

            // Lưu vào database
            notification = notificationRepository.save(notification);

            // Gửi qua WebSocket
            notificationService.sendPrivateNotification(admin.getEmail(), notification);

            log.info("📧 Sent refund notification to admin {}: {}", admin.getEmail(), title);
        }
    }

    /**
     * Format tiền tệ
     */
    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,dđ", amount.longValue());
    }
}
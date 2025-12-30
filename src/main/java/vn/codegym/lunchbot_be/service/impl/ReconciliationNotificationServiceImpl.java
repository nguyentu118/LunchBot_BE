package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.ReconciliationRequest;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.NotificationService;
import vn.codegym.lunchbot_be.service.ReconciliationNotificationService;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static vn.codegym.lunchbot_be.model.enums.UserRole.ADMIN;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReconciliationNotificationServiceImpl implements ReconciliationNotificationService {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    @Override
    @Transactional
    public void notifyAdminNewReconciliationRequest(ReconciliationRequest request) {
        String title = "Yêu cầu đối soát mới #" + request.getId();
        String content = String.format(
                "Merchant %s đã gửi yêu cầu đối soát doanh thu tháng %s. " +
                        "Tổng doanh thu: %,.0f đ. Vui lòng xem xét và phê duyệt.",
                request.getMerchant().getRestaurantName(),
                request.getYearMonth(),
                request.getTotalGrossRevenue()
        );

        notifyAllAdmins(request, title, content, NotificationType.RECONCILIATION_REQUEST_CREATED);
    }

    @Override
    @Transactional
    public void notifyAdminReconciliationClaim(ReconciliationRequest request) {
        String title = "Báo cáo sai sót đối soát #" + request.getId();
        String content = String.format(
                "Merchant %s đã báo cáo sai sót trong đối soát doanh thu tháng %s. " +
                        "Lý do: %s. Vui lòng kiểm tra và xử lý.",
                request.getMerchant().getRestaurantName(),
                request.getYearMonth(),
                request.getMerchantNotes() != null ? request.getMerchantNotes() : "Không có ghi chú"
        );

        notifyAllAdmins(request, title, content, NotificationType.RECONCILIATION_CLAIM_SUBMITTED);
    }

    @Override
    @Transactional
    public void notifyMerchantRequestApproved(ReconciliationRequest request) {
        String title = "Yêu cầu đối soát #" + request.getId() + " đã được phê duyệt";
        String content = String.format(
                "Yêu cầu đối soát doanh thu tháng %s đã được Admin phê duyệt. " +
                        "Doanh thu ròng: %,.0f đ. Số tiền sẽ được chuyển khoản trong 3-5 ngày làm việc.",
                request.getYearMonth(),
                request.getNetRevenue()
        );

        sendNotificationToMerchant(request, title, content, NotificationType.RECONCILIATION_REQUEST_APPROVED);
    }

    @Override
    @Transactional
    public void notifyMerchantRequestRejected(ReconciliationRequest request) {
        String title = "Yêu cầu đối soát #" + request.getId() + " đã bị từ chối";
        String content = String.format(
                "Yêu cầu đối soát doanh thu tháng %s đã bị từ chối. " +
                        "Lý do: %s. %s",
                request.getYearMonth(),
                request.getRejectionReason() != null ? request.getRejectionReason() : "Không có lý do cụ thể",
                request.getAdminNotes() != null ? "Ghi chú: " + request.getAdminNotes() : ""
        );

        sendNotificationToMerchant(request, title, content, NotificationType.RECONCILIATION_REQUEST_REJECTED);
    }

    @Override
    @Transactional
    public void notifyReconciliationStatusChanged(
            ReconciliationRequest request,
            ReconciliationStatus oldStatus,
            ReconciliationStatus newStatus
    ) {
        switch (newStatus) {
            case PENDING:
                notifyAdminNewReconciliationRequest(request);
                break;
            case REPORTED:
                notifyAdminReconciliationClaim(request);
                break;
            case APPROVED:
                notifyMerchantRequestApproved(request);
                break;
            case REJECTED:
                notifyMerchantRequestRejected(request);
                break;
            default:
                log.warn("⚠️ [Reconciliation] Unhandled status: {}", newStatus);
        }
    }

    @Override
    @Transactional
    public void notifyAllAdminsNewRequest(ReconciliationRequest request) {
        notifyAdminNewReconciliationRequest(request);
    }

    /**
     * Gửi thông báo đến Merchant
     */
    private void sendNotificationToMerchant(
            ReconciliationRequest request,
            String title,
            String content,
            NotificationType type
    ) {
        User merchantUser = request.getMerchant().getUser();
        String merchantEmail = merchantUser.getEmail();

        try {
            // Build notification
            Notification notification = Notification.builder()
                    .user(merchantUser)
                    .merchant(request.getMerchant())
                    .title(title)
                    .content(content)
                    .type(type)
                    .isRead(false)
                    .build();

            notification = notificationRepository.save(notification);

            // Send via WebSocket
            notificationService.sendPrivateNotification(merchantEmail, notification);

        } catch (Exception e) {
            log.error("❌ [Merchant] Failed to send notification to: {}", merchantEmail, e);
        }
    }

    /**
     * Gửi thông báo đến tất cả Admin
     */
    private void notifyAllAdmins(
            ReconciliationRequest request,
            String title,
            String content,
            NotificationType type
    ) {
        List<User> adminUsers = userRepository.findByRole(ADMIN);

        if (adminUsers.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (User admin : adminUsers) {
            String adminEmail = admin.getEmail();

            try {
                // Build notification
                Notification notification = Notification.builder()
                        .user(admin)
                        .merchant(request.getMerchant())
                        .title(title)
                        .content(content)
                        .type(type)
                        .isRead(false)
                        .build();

                notification = notificationRepository.save(notification);

                // Send via WebSocket
                notificationService.sendPrivateNotification(adminEmail, notification);

                successCount++;

            } catch (Exception e) {
                failCount++;
            }
        }
    }
}
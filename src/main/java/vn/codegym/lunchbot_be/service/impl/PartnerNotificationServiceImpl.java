package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.NotificationService;
import vn.codegym.lunchbot_be.service.PartnerNotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static vn.codegym.lunchbot_be.model.enums.UserRole.ADMIN;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerNotificationServiceImpl implements PartnerNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void notifyAdminNewPartnerRequest(Merchant merchant) {
        try {
            // 1. Tìm tất cả admin
            List<User> admins = userRepository.findByRole(ADMIN);

            if (admins.isEmpty()) {
                log.warn("⚠️ Không tìm thấy admin nào để gửi thông báo");
                return;
            }

            // 2. Tạo nội dung thông báo
            String title = "🔔 Yêu cầu đối tác thân thiết mới";
            String content = String.format(
                    "Nhà hàng \"%s\" đã đăng ký trở thành đối tác thân thiết. " +
                            "Vui lòng kiểm tra và phê duyệt.",
                    merchant.getRestaurantName()
            );

            // 3. Gửi thông báo cho từng admin
            for (User admin : admins) {
                Notification notification = Notification.builder()
                        .user(admin)
                        .merchant(merchant)
                        .title(title)
                        .content(content)
                        .type(NotificationType.PARTNER_REQUEST)
                        .sentAt(LocalDateTime.now())
                        .isRead(false)
                        .build();

                // Lưu vào DB
                notificationRepository.save(notification);

                // Gửi qua WebSocket
                notificationService.sendPrivateNotification(admin.getEmail(), notification);

                log.info("✅ Đã gửi thông báo đăng ký đối tác cho admin: {}", admin.getEmail());
            }

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo cho admin về yêu cầu đối tác", e);
        }
    }

    @Override
    @Transactional
    public void notifyMerchantPartnerApproved(Merchant merchant) {
        try {
            User merchantUser = merchant.getUser();

            if (merchantUser == null) {
                log.warn("⚠️ Không tìm thấy user của merchant ID: {}", merchant.getId());
                return;
            }

            // Tạo thông báo chúc mừng
            String title = "🎉 Chúc mừng! Đối tác thân thiết";
            String content = String.format(
                    "Xin chúc mừng \"%s\"! Yêu cầu trở thành đối tác thân thiết của bạn đã được phê duyệt. " +
                            "Bạn sẽ được hưởng mức phí hoa hồng ưu đãi %.2f%%.",
                    merchant.getRestaurantName(),
                    merchant.getCommissionRate().multiply(new java.math.BigDecimal("100"))
            );

            Notification notification = Notification.builder()
                    .user(merchantUser)
                    .merchant(merchant)  // ✅ THÊM merchant_id
                    .title(title)
                    .content(content)
                    .type(NotificationType.PARTNER_APPROVED)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            // Lưu vào DB
            notificationRepository.save(notification);

            // Gửi qua WebSocket
            notificationService.sendPrivateNotification(merchantUser.getEmail(), notification);

            log.info("✅ Đã gửi thông báo phê duyệt đối tác cho merchant: {}", merchantUser.getEmail());

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo phê duyệt cho merchant ID: {}", merchant.getId(), e);
        }
    }

    @Override
    @Transactional
    public void notifyMerchantPartnerRejected(Merchant merchant, String reason) {
        try {
            User merchantUser = merchant.getUser();

            if (merchantUser == null) {
                log.warn("⚠️ Không tìm thấy user của merchant ID: {}", merchant.getId());
                return;
            }

            // Tạo thông báo từ chối
            String title = "❌ Yêu cầu đối tác thân thiết bị từ chối";
            String content = String.format(
                    "Rất tiếc, yêu cầu trở thành đối tác thân thiết của \"%s\" chưa được chấp thuận. " +
                            "Lý do: %s. Bạn có thể đăng ký lại sau khi đáp ứng đủ điều kiện.",
                    merchant.getRestaurantName(),
                    reason != null && !reason.isEmpty() ? reason : "Chưa đáp ứng đủ điều kiện"
            );

            Notification notification = Notification.builder()
                    .user(merchantUser)
                    .merchant(merchant)  // ✅ THÊM merchant_id
                    .title(title)
                    .content(content)
                    .type(NotificationType.PARTNER_REJECTED)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();

            // Lưu vào DB
            notificationRepository.save(notification);

            // Gửi qua WebSocket
            notificationService.sendPrivateNotification(merchantUser.getEmail(), notification);

            log.info("✅ Đã gửi thông báo từ chối đối tác cho merchant: {}", merchantUser.getEmail());

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi thông báo từ chối cho merchant ID: {}", merchant.getId(), e);
        }
    }
}
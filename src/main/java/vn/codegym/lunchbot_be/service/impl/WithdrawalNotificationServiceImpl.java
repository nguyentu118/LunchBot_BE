// WithdrawalNotificationServiceImpl.java
package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.WithdrawalRequest;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.NotificationService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static vn.codegym.lunchbot_be.model.enums.UserRole.ADMIN;

@Service
@Slf4j
@RequiredArgsConstructor
public class WithdrawalNotificationServiceImpl {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Transactional
    public void notifyMerchantWithdrawalRequested(WithdrawalRequest request) {
        String title = "Yêu cầu rút tiền đã được gửi";
        String content = String.format(
                "Yêu cầu rút %s đã được gửi thành công. " +
                        "Chúng tôi sẽ xử lý trong vòng 1-3 ngày làm việc.",
                formatCurrency(request.getAmount())
        );

        sendNotificationToMerchant(
                request,
                title,
                content,
                NotificationType.WITHDRAWAL_REQUESTED
        );
    }

    @Transactional
    public void notifyAdminNewWithdrawalRequest(WithdrawalRequest request) {
        String title = "Yêu cầu rút tiền mới #" + request.getId();
        String content = String.format(
                "Merchant %s yêu cầu rút %s. Ngân hàng: %s - STK: %s",
                request.getMerchant().getRestaurantName(),
                formatCurrency(request.getAmount()),
                request.getMerchant().getBankName(),
                maskAccountNumber(request.getMerchant().getBankAccountNumber())
        );

        sendNotificationToAllAdmins(
                request,
                title,
                content,
                NotificationType.WITHDRAWAL_REQUESTED
        );
    }

    @Transactional
    public void notifyMerchantWithdrawalApproved(WithdrawalRequest request) {
        String title = "Yêu cầu rút tiền đã được duyệt ✓";
        String content = String.format(
                "Yêu cầu rút %s đã được duyệt. " +
                        "Tiền sẽ được chuyển vào tài khoản %s - %s trong 1-2 ngày làm việc.",
                formatCurrency(request.getAmount()),
                request.getMerchant().getBankName(),
                maskAccountNumber(request.getMerchant().getBankAccountNumber())
        );

        sendNotificationToMerchant(
                request,
                title,
                content,
                NotificationType.WITHDRAWAL_APPROVED
        );
    }

    @Transactional
    public void notifyMerchantWithdrawalRejected(WithdrawalRequest request) {
        String title = "Yêu cầu rút tiền bị từ chối";
        String content = String.format(
                "Rất tiếc, yêu cầu rút %s đã bị từ chối. " +
                        "Lý do: %s. Số tiền đã được hoàn lại vào số dư của bạn.",
                formatCurrency(request.getAmount()),
                request.getAdminNotes() != null ? request.getAdminNotes() : "Không có lý do cụ thể"
        );

        sendNotificationToMerchant(
                request,
                title,
                content,
                NotificationType.WITHDRAWAL_REJECTED
        );
    }

    @Transactional
    public void notifyMerchantContractLiquidated(WithdrawalRequest request) {
        String title = "Thanh lý hợp đồng hoàn tất";
        String content = String.format(
                "Yêu cầu thanh lý hợp đồng đã được xử lý. " +
                        "Số tiền %s sẽ được chuyển vào tài khoản của bạn. " +
                        "Cảm ơn bạn đã đồng hành cùng chúng tôi!",
                formatCurrency(request.getAmount())
        );

        sendNotificationToMerchant(
                request,
                title,
                content,
                NotificationType.CONTRACT_LIQUIDATED
        );
    }

    @Transactional
    public void notifyAdminContractLiquidation(WithdrawalRequest request) {
        String title = "⚠️ Yêu cầu thanh lý hợp đồng #" + request.getId();
        String content = String.format(
                "Merchant %s yêu cầu THANH LÝ HỢP ĐỒNG. " +
                        "Số tiền rút toàn bộ: %s. Vui lòng xử lý ưu tiên.",
                request.getMerchant().getRestaurantName(),
                formatCurrency(request.getAmount())
        );

        sendNotificationToAllAdmins(
                request,
                title,
                content,
                NotificationType.CONTRACT_LIQUIDATED
        );
    }

    // ============= PRIVATE HELPER METHODS =============

    private void sendNotificationToMerchant(
            WithdrawalRequest request,
            String title,
            String content,
            NotificationType type
    ) {
        User merchantUser = request.getMerchant().getUser();

        Notification notification = Notification.builder()
                .user(merchantUser)
                .merchant(request.getMerchant())
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .build();

        // Lưu vào database
        notification = notificationRepository.save(notification);

        // Gửi qua WebSocket
        notificationService.sendPrivateNotification(merchantUser.getEmail(), notification);

        log.info("✅ Sent withdrawal notification to merchant {}: {}",
                merchantUser.getEmail(), title);
    }

    private void sendNotificationToAllAdmins(
            WithdrawalRequest request,
            String title,
            String content,
            NotificationType type
    ) {
        // Lấy tất cả Admin users
        List<User> adminUsers = userRepository.findByRole(ADMIN);

        if (adminUsers.isEmpty()) {
            log.warn("⚠️ No admin users found to notify about withdrawal request #{}",
                    request.getId());
            return;
        }

        for (User admin : adminUsers) {
            Notification notification = Notification.builder()
                    .user(admin)
                    .merchant(request.getMerchant())
                    .title(title)
                    .content(content)
                    .type(type)
                    .isRead(false)
                    .build();

            // Lưu vào database
            notification = notificationRepository.save(notification);

            // Gửi qua WebSocket
            notificationService.sendPrivateNotification(admin.getEmail(), notification);

            log.info("✅ Sent withdrawal notification to admin {}: {}",
                    admin.getEmail(), title);
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0đ";
        }
        return String.format("%,.0fđ", amount);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        int visibleDigits = 4;
        int maskedLength = accountNumber.length() - visibleDigits;
        return "*".repeat(maskedLength) + accountNumber.substring(maskedLength);
    }
}
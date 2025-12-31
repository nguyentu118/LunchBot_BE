package vn.codegym.lunchbot_be.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.codegym.lunchbot_be.service.EmailService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@lunchbot.com}")
    private String fromEmail;

    @Value("${app.mail.support:support@lunchbot.com}")
    private String supportEmail;

    @Value("${app.name:LunchBot}")
    private String appName;

    @Value("${app.url:http://localhost:5173/login}")
    private String appUrl;

    private final JavaMailSender mailSender;

    private final ResourceLoader resourceLoader;

    private static final Logger LOGGER = Logger.getLogger(EmailServiceImpl.class.getName());
    @Async // Đảm bảo việc gửi email không làm chậm request API
    public void sendVerificationEmail(String to, String fullName, String token) {
        // Sử dụng templateEngine.process
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : to);

            // Link kích hoạt trỏ về Backend endpoint /api/auth/verify
            // Đảm bảo URL này là domain/host thực tế của Backend (Ví dụ: https://api.lunchbot.vn/api/auth/verify?token=...)
            String verificationLink = "http://localhost:5173/login?token=" + token;

            context.setVariable("verificationLink", verificationLink);
            context.setVariable("appName", appName); // Sử dụng biến appName nếu có
            context.setVariable("currentYear", String.valueOf(Year.now().getValue())); // Sử dụng biến Year

            String htmlContent = templateEngine.process("emails/email-verification", context);

            sendHtmlEmail(to,
                    "✅ Xác thực Email để kích hoạt tài khoản LunchBot",
                    htmlContent);

            log.info("Email kích hoạt thành công tới: {}", to);

        } catch (Exception e) {
            log.error("Lỗi khi gửi email kích hoạt tới {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email kích hoạt.", e);
        }
    }


    @Override
    @Async
    public void sendShippingPartnerLockedEmail(String partnerEmail, String partnerName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("partnerName", partnerName);
            context.setVariable("reason", reason != null ? reason : "Vi phạm chính sách dịch vụ");
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            log.info("🔴 Đang gửi email LOCKED từ template: emails/shipping-partner-locked");
            String htmlContent = templateEngine.process("emails/shipping-partner-locked", context);

            sendHtmlEmail(partnerEmail,
                    "🚫 Thông báo khóa tài khoản đối tác vận chuyển",
                    htmlContent);

            log.info("✅ Shipping partner LOCKED email sent to: {}", partnerEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send shipping partner LOCKED email to {}: {}", partnerEmail, e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendShippingPartnerUnlockedEmail(String partnerEmail, String partnerName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("partnerName", partnerName);
            context.setVariable("reason", reason != null ? reason : "Tài khoản đã được mở khóa");
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            log.info("🟢 Đang gửi email UNLOCKED từ template: emails/shipping-partner-unlocked");
            String htmlContent = templateEngine.process("emails/shipping-partner-unlocked", context);

            sendHtmlEmail(partnerEmail,
                    "✅ Thông báo mở khóa tài khoản đối tác vận chuyển",
                    htmlContent);

            log.info("✅ Shipping partner UNLOCKED email sent to: {}", partnerEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send shipping partner UNLOCKED email to {}: {}", partnerEmail, e.getMessage(), e);
        }
    }


    @Async
    public void sendRegistrationSuccessEmail(String to, String fullName, String restaurantName, String loginUrl, boolean isMerchant) {
        try {
            Context context = new Context();
            context.setVariable("fullName", fullName != null ? fullName : to);
            context.setVariable("email", to);
            context.setVariable("restaurantName", restaurantName != null ? restaurantName : "");
            context.setVariable("appUrl", loginUrl);
            context.setVariable("currentYear", String.valueOf(Year.now().getValue()));
            context.setVariable("appName", appName);

            // Chọn template dựa trên vai trò
            String templateName = isMerchant
                    ? "emails/merchant_registration_template"
                    : "emails/user_registration_template";

            String htmlContent = templateEngine.process(templateName, context);

            // Đặt Subject dựa trên vai trò
            String subject = isMerchant
                    ? "🎉 Đăng Ký Merchant Thành Công trên LunchBot"
                    : "👋 Chào Mừng Đến Với LunchBot!";

            sendHtmlEmail(to, subject, htmlContent);

            log.info("Registration email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send registration email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email thông báo.", e);
        }
    }

    @Override
    public void sendMerchantApprovalEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
            context.setVariable("merchantEmail", merchantEmail);
            context.setVariable("restaurantName", restaurantName);
            context.setVariable("reason", reason != null ? reason : "Hồ sơ đã đạt yêu cầu");
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/merchant-approval", context);

            sendHtmlEmail(merchantEmail,
                    "🎉 Chúc mừng! Tài khoản merchant của bạn đã được phê duyệt",
                    htmlContent);
            log.info(merchantEmail);
            log.info("Merchant approval email sent successfully to: {}", merchantEmail);

        } catch (Exception e) {
            log.error("Failed to send merchant approval email to {}: {}", merchantEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendMerchantRejectionEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
            context.setVariable("merchantEmail", merchantEmail);
            context.setVariable("restaurantName", restaurantName);
            context.setVariable("reason", reason != null ? reason : "Hồ sơ chưa đạt yêu cầu");
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/merchant-rejection", context);

            sendHtmlEmail(merchantEmail,
                    "❌ Thông báo về việc xét duyệt tài khoản merchant",
                    htmlContent);

            log.info("Merchant rejection email sent successfully to: {}", merchantEmail);

        } catch (Exception e) {
            log.error("Failed to send merchant rejection email to {}: {}", merchantEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendMerchantLockedEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
            context.setVariable("restaurantName", restaurantName);
            context.setVariable("reason", reason != null ? reason : "Vi phạm chính sách");
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/merchant-locked", context);

            sendHtmlEmail(merchantEmail,
                    "🚫 Thông báo khóa tài khoản merchant",
                    htmlContent);

            log.info("Merchant locked email sent successfully to: {}", merchantEmail);

        } catch (Exception e) {
            log.error("Failed to send merchant locked email to {}: {}", merchantEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendMerchantUnlockedEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
            context.setVariable("merchantEmail", merchantEmail);
            context.setVariable("restaurantName", restaurantName);
            context.setVariable("reason", reason != null ? reason : "Tài khoản đã được mở khóa");
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/merchant-unlocked", context);

            sendHtmlEmail(merchantEmail,
                    "✅ Thông báo mở khóa tài khoản merchant",
                    htmlContent);

            log.info("Merchant unlocked email sent successfully to: {}", merchantEmail);

        } catch (Exception e) {
            log.error("Failed to send merchant unlocked email to {}: {}", merchantEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendWelcomeEmail(String userEmail, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("emails/welcome", context);

            sendHtmlEmail(userEmail,
                    "🎉 Chào mừng bạn đến với " + appName,
                    htmlContent);

            log.info("Welcome email sent successfully to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", userEmail, e.getMessage(), e);
        }

    }

    @Override
    public void sendPasswordResetEmail(String userEmail, String userName, String resetToken) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("resetLink", appUrl + "/reset-password?token=" + resetToken);
            context.setVariable("appName", appName);
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("emails/password-reset", context);

            sendHtmlEmail(userEmail,
                    "🔐 Yêu cầu đặt lại mật khẩu",
                    htmlContent);

            log.info("Password reset email sent successfully to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", userEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendOrderConfirmationEmail(String userEmail, String userName, String orderDetails) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderDetails", orderDetails);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/order-confirmation", context);

            sendHtmlEmail(userEmail,
                    "📦 Xác nhận đơn hàng từ " + appName,
                    htmlContent);

            log.info("Order confirmation email sent successfully to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send order confirmation email to {}: {}", userEmail, e.getMessage(), e);
        }
    }

    @Override
    public void sendOrderStatusUpdateEmail(String userEmail, String userName, String orderStatus, String orderDetails) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderStatus", orderStatus);
            context.setVariable("orderDetails", orderDetails);
            context.setVariable("appName", appName);
            context.setVariable("appUrl", appUrl);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            String htmlContent = templateEngine.process("emails/order-status-update", context);

            sendHtmlEmail(userEmail,
                    "📮 Cập nhật trạng thái đơn hàng",
                    htmlContent);

            log.info("Order status update email sent successfully to: {}", userEmail);

        } catch (Exception e) {
            log.error("Failed to send order status update email to {}: {}", userEmail, e.getMessage(), e);
        }
    }
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    private void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage(), e);
        }
    }
}
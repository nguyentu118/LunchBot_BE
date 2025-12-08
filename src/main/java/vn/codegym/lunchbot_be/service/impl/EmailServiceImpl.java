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

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    // Lưu ý: @RequiredArgsConstructor sẽ tự động inject qua constructor
    // nhưng bạn vẫn có thể giữ @Autowired ở đây
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ResourceLoader resourceLoader;

    private static final Logger LOGGER = Logger.getLogger(EmailServiceImpl.class.getName());

    // ----------------------------------------------------------------------
    // PHƯƠNG THỨC GỬI EMAIL HTML (SỬ DỤNG MIME MESSAGE)
    // ----------------------------------------------------------------------
    public void sendRegistrationSuccessEmail(String to, String fullName, String restaurantName, String loginUrl) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("🎉 Đăng Ký Merchant Thành Công trên LunchBot");

            String htmlContent = buildHtmlContent(to, fullName, restaurantName, loginUrl);

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            LOGGER.log(Level.INFO, "Gửi email thành công tới: {0}", to);

        } catch (MailException | MessagingException exception) {
            LOGGER.log(Level.SEVERE, "Lỗi khi gửi email HTML tới: " + to, exception);
            throw new RuntimeException("Không thể gửi email thông báo HTML.", exception);
        }
    }

    // ----------------------------------------------------------------------
    // HÀM XÂY DỰNG NỘI DUNG HTML
    // ----------------------------------------------------------------------
    private String buildHtmlContent(String email, String fullName, String restaurantName, String loginUrl) {
        String template = readTemplateFile("classpath:templates/emails/registration_success_template.html");

        // Thay thế các biến động
        return template
                .replace("${fullName}", fullName != null ? fullName : email)
                .replace("${restaurantName}", restaurantName)
                .replace("${email}", email)
                .replace("${loginUrl}", loginUrl)
                .replace("${currentYear}", String.valueOf(Year.now().getValue()));
    }

    // ----------------------------------------------------------------------
    // HÀM ĐỌC FILE TEMPLATE
    // ----------------------------------------------------------------------
    private String readTemplateFile(String filePath) {
        try {
            Resource resource = resourceLoader.getResource(filePath);

            try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(reader);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Không thể đọc file template: " + filePath, e);
            return "<h1>Lỗi: Không tìm thấy template email.</h1>";
        }
    }


    @Override
    public void sendMerchantApprovalEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
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

            log.info("Merchant approval email sent successfully to: {}", merchantEmail);

        } catch (Exception e) {
            log.error("Failed to send merchant approval email to {}: {}", merchantEmail, e.getMessage(), e);
            // Fallback to simple email
            sendSimpleMerchantApprovalEmail(merchantEmail, merchantName, restaurantName, reason);
        }
    }

    @Override
    public void sendMerchantRejectionEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
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
            // Fallback to simple email
            sendSimpleMerchantRejectionEmail(merchantEmail, merchantName, restaurantName, reason);
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
            // Fallback to simple email
            sendSimpleMerchantLockedEmail(merchantEmail, merchantName, restaurantName, reason);
        }
    }

    @Override
    public void sendMerchantUnlockedEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        try {
            Context context = new Context();
            context.setVariable("merchantName", merchantName);
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
            // Fallback to simple email
            sendSimpleMerchantUnlockedEmail(merchantEmail, merchantName, restaurantName, reason);
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
    private void sendSimpleMerchantApprovalEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        String subject = "🎉 Chúc mừng! Tài khoản merchant của bạn đã được phê duyệt";
        String content = String.format(
                "Kính chào %s,\n\n" +
                        "Chúc mừng! Tài khoản merchant cho nhà hàng \"%s\" của bạn đã được phê duyệt thành công.\n\n" +
                        "Lý do: %s\n\n" +
                        "Bạn có thể đăng nhập và bắt đầu quản lý nhà hàng của mình ngay bây giờ.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ %s\n" +
                        "Email hỗ trợ: %s",
                merchantName, restaurantName, reason, appName, supportEmail
        );

        sendSimpleEmail(merchantEmail, subject, content);
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
    private void sendSimpleMerchantRejectionEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        String subject = "❌ Thông báo về việc xét duyệt tài khoản merchant";
        String content = String.format(
                "Kính chào %s,\n\n" +
                        "Chúng tôi rất tiếc phải thông báo rằng tài khoản merchant cho nhà hàng \"%s\" của bạn chưa được phê duyệt.\n\n" +
                        "Lý do: %s\n\n" +
                        "Vui lòng kiểm tra và cập nhật thông tin theo yêu cầu, sau đó gửi lại đơn đăng ký.\n\n" +
                        "Nếu có thắc mắc, vui lòng liên hệ với chúng tôi.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ %s\n" +
                        "Email hỗ trợ: %s",
                merchantName, restaurantName, reason, appName, supportEmail
        );

        sendSimpleEmail(merchantEmail, subject, content);
    }
    private void sendSimpleMerchantLockedEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        String subject = "🚫 Thông báo khóa tài khoản merchant";
        String content = String.format(
                "Kính chào %s,\n\n" +
                        "Chúng tôi phải thông báo rằng tài khoản merchant cho nhà hàng \"%s\" của bạn đã bị khóa tạm thời.\n\n" +
                        "Lý do: %s\n\n" +
                        "Vui lòng liên hệ với đội ngũ hỗ trợ để được giải quyết vấn đề.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ %s\n" +
                        "Email hỗ trợ: %s",
                merchantName, restaurantName, reason, appName, supportEmail
        );

        sendSimpleEmail(merchantEmail, subject, content);
    }
    private void sendSimpleMerchantUnlockedEmail(String merchantEmail, String merchantName, String restaurantName, String reason) {
        String subject = "✅ Thông báo mở khóa tài khoản merchant";
        String content = String.format(
                "Kính chào %s,\n\n" +
                        "Chúng tôi vui mừng thông báo rằng tài khoản merchant cho nhà hàng \"%s\" của bạn đã được mở khóa.\n\n" +
                        "Lý do: %s\n\n" +
                        "Bạn có thể tiếp tục sử dụng dịch vụ như bình thường.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ %s\n" +
                        "Email hỗ trợ: %s",
                merchantName, restaurantName, reason, appName, supportEmail
        );

        sendSimpleEmail(merchantEmail, subject, content);
    }
}
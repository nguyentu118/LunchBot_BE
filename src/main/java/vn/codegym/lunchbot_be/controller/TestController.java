// controller/TestController.java
package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final JavaMailSender mailSender;

    @GetMapping("/email")
    public String testEmail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("nguyenduc8197@gmail.com");
            message.setTo("nguyenduc8197@gmail.com");
            message.setSubject("TEST EMAIL - LunchBot");
            message.setText("Nếu bạn nhận được email này, cấu hình email ĐÚNG!");

            mailSender.send(message);
            return "✅ Email đã gửi! Kiểm tra hộp thư của bạn.";
        } catch (Exception e) {
            return "❌ Lỗi: " + e.getMessage();
        }
    }
}

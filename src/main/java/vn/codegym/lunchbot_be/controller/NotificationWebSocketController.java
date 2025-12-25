package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class NotificationWebSocketController {

    /**
     * WebSocket endpoint để client đăng ký nhận thông báo
     * Client sẽ subscribe tới: /user/queue/notifications
     */
    @MessageMapping("/notifications/subscribe")
    public void subscribeToNotifications(SimpMessageHeaderAccessor headerAccessor, Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            System.out.println("User " + email + " subscribed to notifications");
        }
    }
}
package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.response.NotificationResponse;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.NotificationService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    private final UserRepository userRepository;

    @Override
    public void sendPrivateNotification(String username, Notification notification) {


        NotificationResponse response = NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType().name())
                .sentAt(notification.getSentAt())
                .build();

        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/notifications",
                response);
    }


}

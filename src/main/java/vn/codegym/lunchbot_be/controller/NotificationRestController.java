package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.response.NotificationResponse;
import vn.codegym.lunchbot_be.exception.NotificationNotFoundException;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.repository.NotificationRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationRepository notificationRepository;

    /**
     * REST API để lấy danh sách thông báo của user
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        String email = authentication.getName();

        List<Notification> notifications = notificationRepository
                .findByUserEmailOrderBySentAtDesc(email);

        List<NotificationResponse> responses = notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * REST API để lấy thông báo chưa đọc
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(Authentication authentication) {
        String email = authentication.getName();

        List<Notification> notifications = notificationRepository
                .findByUserEmailAndIsReadFalseOrderBySentAtDesc(email);

        List<NotificationResponse> responses = notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * REST API để đếm số thông báo chưa đọc
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(Authentication authentication) {
        String email = authentication.getName();
        long count = notificationRepository.countByUserEmailAndIsReadFalse(email);
        return ResponseEntity.ok(count);
    }

    /**
     * REST API để đánh dấu thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        // Kiểm tra notification có thuộc về user không
        if (!notification.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(403).build();
        }

        notification.markAsRead();
        notificationRepository.save(notification);

        return ResponseEntity.ok().build();
    }

    /**
     * REST API để đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String email = authentication.getName();

        List<Notification> notifications = notificationRepository
                .findByUserEmailAndIsReadFalse(email);

        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);

        return ResponseEntity.ok().build();
    }

    /**
     * REST API để xóa thông báo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));

        // Kiểm tra notification có thuộc về user không
        if (!notification.getUser().getEmail().equals(email)) {
            return ResponseEntity.status(403).build();
        }

        notificationRepository.delete(notification);

        return ResponseEntity.ok().build();
    }

    private NotificationResponse convertToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType().name())
                .isRead(notification.getIsRead())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
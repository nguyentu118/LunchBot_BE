package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.CancelledBy;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.repository.NotificationRepository;
import vn.codegym.lunchbot_be.service.NotificationService;
import vn.codegym.lunchbot_be.service.OrderNotificationService;

import java.time.format.DateTimeFormatter;


@Service
@Slf4j
@RequiredArgsConstructor
public class OrderNotificationServiceImpl implements OrderNotificationService {

    private final NotificationService notificationService;

    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");


    @Override
    @Transactional
    public void notifyMerchantNewOrder(Order order) {
        String title = "Đơn hàng mới #" + order.getId();
        String content = String.format(
                "Bạn có đơn hàng mới từ %s. Tổng giá trị: %,.0f đ. Vui lòng xác nhận đơn hàng.",
                order.getUser().getEmail(),
                order.getTotalAmount()
        );

        sendNotificationToMerchant(order, title, content, NotificationType.ORDER_CREATED);
    }

    @Override
    @Transactional
    public void notifyMerchantOrderCancelled(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đã bị hủy";
        String content = String.format(
                "Khách hàng %s đã hủy đơn hàng. Lý do: %s",
                order.getUser().getEmail(),
                order.getCancellationReason() != null ? order.getCancellationReason() : "Không có lý do"
        );

        sendNotificationToMerchant(order, title, content, NotificationType.ORDER_CANCELLED);
    }

    @Override
    @Transactional
    public void notifyUserOrderConfirmed(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đã được xác nhận";
        String content = String.format(
                "Cửa hàng %s đã xác nhận đơn hàng của bạn. Đơn hàng sẽ được chuẩn bị sớm nhất có thể.",
                order.getMerchant().getUser().getEmail()
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_CONFIRMED);
    }

    @Override
    @Transactional
    public void notifyUserOrderRejected(Order order, String reason) {
        String title = "Đơn hàng #" + order.getId() + " đã bị từ chối";
        String content = String.format(
                "Rất tiếc, cửa hàng %s đã từ chối đơn hàng của bạn. Lý do: %s. Số tiền sẽ được hoàn lại trong 1-3 ngày làm việc.",
                order.getMerchant().getUser().getEmail(),
                reason != null ? reason : "Không có lý do cụ thể"
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_CANCELLED);
    }

    @Override
    @Transactional
    public void notifyUserOrderPreparing(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đang được chuẩn bị";
        String content = String.format(
                "Cửa hàng %s đang chuẩn bị đơn hàng của bạn. Thời gian dự kiến hoàn thành: %s",
                order.getMerchant().getUser().getEmail(),
                order.getExpectedDeliveryTime() != null ?
                        order.getExpectedDeliveryTime().format(TIME_FORMATTER) : "đang cập nhật"
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_PREPARING);
    }

    @Override
    @Transactional
    public void notifyUserOrderReady(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đã sẵn sàng";
        String content = String.format(
                "Đơn hàng của bạn tại %s đã sẵn sàng và sẽ được giao trong thời gian sớm nhất.",
                order.getMerchant().getUser().getEmail()
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_READY);
    }

    @Override
    @Transactional
    public void notifyUserOrderDelivering(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đang được giao";
        String content = String.format(
                "Đơn hàng của bạn đang trên đường giao đến. Thời gian dự kiến: %s",
                order.getExpectedDeliveryTime() != null ?
                        order.getExpectedDeliveryTime().format(TIME_FORMATTER) : "đang cập nhật"
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_DELIVERING);
    }

    @Override
    @Transactional
    public void notifyUserOrderCompleted(Order order) {
        String title = "Đơn hàng #" + order.getId() + " đã hoàn thành";
        String content = String.format(
                "Đơn hàng của bạn đã được giao thành công. Cảm ơn bạn đã sử dụng dịch vụ! Đánh giá đơn hàng để nhận ưu đãi nhé."
        );

        sendNotificationToUser(order, title, content, NotificationType.ORDER_COMPLETED);
    }

    @Override
    @Transactional
    public void notifyOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        log.info("Order #{} status changed from {} to {}", order.getId(), oldStatus, newStatus);

        switch (newStatus) {
            case PENDING:
                notifyMerchantNewOrder(order);
                break;
            case CONFIRMED:
                notifyUserOrderConfirmed(order);
                break;
            case PROCESSING:
                notifyUserOrderPreparing(order);
                break;
            case READY:
                notifyUserOrderReady(order);
                break;
            case DELIVERING:
                notifyUserOrderDelivering(order);
                break;
            case COMPLETED:
                notifyUserOrderCompleted(order);
                break;
            case CANCELLED:
                if (order.getCancelledBy() == CancelledBy.CUSTOMER) {
                    // User hủy → Thông báo cho Merchant
                    notifyMerchantOrderCancelled(order);
                } else if (order.getCancelledBy() == CancelledBy.MERCHANT) {
                    // Merchant hủy → Thông báo cho User
                    notifyUserOrderRejected(order, order.getCancellationReason());
                } else {
                    log.warn("Order #{} cancelled but cancelledBy is null", order.getId());
                }
                break;
            default:
                log.warn("Unhandled order status: {}", newStatus);
        }
    }

    private void sendNotificationToUser(Order order, String title, String content, NotificationType type) {
        User user = order.getUser();

        Notification notification = Notification.builder()
                .user(user)
                .merchant(order.getMerchant())
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .build();

        // Lưu vào database
        notification = notificationRepository.save(notification);

        // Gửi qua WebSocket
        notificationService.sendPrivateNotification(user.getEmail(), notification);

        log.info("Sent notification to user {}: {}", user.getEmail(), title);
    }

    private void sendNotificationToMerchant(Order order, String title, String content, NotificationType type) {
        // Giả sử merchant có username hoặc email để nhận thông báo
        // Bạn cần điều chỉnh theo cấu trúc Merchant model của mình
        String merchantUsername = order.getMerchant().getUser().getEmail(); // hoặc getUsername()

        User merchantUser = order.getMerchant().getUser();

        Notification notification = Notification.builder()
                .user(merchantUser)
                .merchant(order.getMerchant())
                .title(title)
                .content(content)
                .type(type)
                .isRead(false)
                .build();

        // Lưu vào database
        notification = notificationRepository.save(notification);

        // Gửi qua WebSocket đến merchant
        notificationService.sendPrivateNotification(merchantUsername, notification);

        log.info("Sent notification to merchant {}: {}", merchantUsername, title);
    }
}

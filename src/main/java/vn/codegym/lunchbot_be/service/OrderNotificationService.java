package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;

public interface OrderNotificationService {
    /**
     * Gửi thông báo cho merchant khi có đơn hàng mới
     */
    void notifyMerchantNewOrder(Order order);

    /**
     * Gửi thông báo cho merchant khi user hủy đơn
     */
    void notifyMerchantOrderCancelled(Order order);

    /**
     * Gửi thông báo cho user khi merchant xác nhận đơn
     */
    void notifyUserOrderConfirmed(Order order);

    /**
     * Gửi thông báo cho user khi merchant từ chối đơn
     */
    void notifyUserOrderRejected(Order order, String reason);

    /**
     * Gửi thông báo cho user khi đơn hàng đang được chuẩn bị
     */
    void notifyUserOrderPreparing(Order order);

    /**
     * Gửi thông báo cho user khi đơn hàng sẵn sàng để giao
     */
    void notifyUserOrderReady(Order order);

    /**
     * Gửi thông báo cho user khi đơn hàng đang được giao
     */
    void notifyUserOrderDelivering(Order order);

    /**
     * Gửi thông báo cho user khi đơn hàng đã hoàn thành
     */
    void notifyUserOrderCompleted(Order order);

    /**
     * Gửi thông báo chung khi trạng thái đơn hàng thay đổi
     */
    void notifyOrderStatusChanged(Order order, OrderStatus oldStatus, OrderStatus newStatus);
}

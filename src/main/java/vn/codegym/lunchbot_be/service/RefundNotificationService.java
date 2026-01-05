package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.model.RefundRequest;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;

public interface RefundNotificationService {

    /**
     * Thông báo cho admin khi có yêu cầu hoàn tiền mới
     */
    void notifyAdminNewRefundRequest(RefundRequest refundRequest);

    /**
     * Thông báo cho user khi admin bắt đầu xử lý hoàn tiền
     */
    void notifyUserRefundProcessing(RefundRequest refundRequest);

    /**
     * Thông báo cho user khi hoàn tiền thành công
     */
    void notifyUserRefundCompleted(RefundRequest refundRequest);

    /**
     * Thông báo cho user khi hoàn tiền thất bại
     */
    void notifyUserRefundFailed(RefundRequest refundRequest, String reason);

    /**
     * Thông báo cho user khi yêu cầu hoàn tiền bị hủy
     */
    void notifyUserRefundCancelled(RefundRequest refundRequest, String reason);

    /**
     * Thông báo tổng quát khi trạng thái hoàn tiền thay đổi
     */
    void notifyRefundStatusChanged(RefundRequest refundRequest, RefundStatus oldStatus, RefundStatus newStatus);
}
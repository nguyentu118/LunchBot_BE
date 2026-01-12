package vn.codegym.lunchbot_be.model.enums;

public enum NotificationType {
    SYSTEM,
    PROMOTION,

    // Thông báo về đơn hàng
    ORDER_CREATED,          // Đơn hàng mới được tạo
    ORDER_CONFIRMED,        // Đơn hàng được xác nhận
    ORDER_PREPARING,        // Đơn hàng đang được chuẩn bị
    ORDER_READY,            // Đơn hàng sẵn sàng
    ORDER_DELIVERING,       // Đơn hàng đang được giao
    ORDER_COMPLETED,        // Đơn hàng hoàn thành
    ORDER_CANCELLED,        // Đơn hàng bị hủy

    // Thông báo về thanh toán
    PAYMENT_SUCCESS,        // Thanh toán thành công
    PAYMENT_FAILED,         // Thanh toán thất bại
    REFUND_PROCESSED,       // Hoàn tiền đã được xử lý

    // Thông báo về khuyến mãi
    PROMOTION_NEW,          // Khuyến mãi mới
    PROMOTION_EXPIRING,     // Khuyến mãi sắp hết hạn

    PARTNER_REQUEST,    // Thông báo cho admin về yêu cầu đối tác mới
    PARTNER_APPROVED,   // Thông báo cho merchant khi được duyệt
    PARTNER_REJECTED,   // Thông báo cho merchant khi bị từ chối

    // Thông báo hệ thống
    SYSTEM_ANNOUNCEMENT,    // Thông báo hệ thống
    SYSTEM_MAINTENANCE,     // Bảo trì hệ thống

    // Thông báo khác
    GENERAL,                 // Thông báo chung

    RECONCILIATION_REQUEST_CREATED,    // Merchant tạo yêu cầu mới
    RECONCILIATION_REQUEST_APPROVED,   // Admin approve
    RECONCILIATION_REQUEST_REJECTED,   // Admin reject
    RECONCILIATION_CLAIM_SUBMITTED,    // Merchant báo cáo sai sót

    // Refund notifications - ✅ MỚI THÊM
    REFUND_REQUESTED,      // Admin nhận thông báo có yêu cầu hoàn tiền mới
    REFUND_PROCESSING,     // User nhận thông báo đang xử lý
    REFUND_COMPLETED,      // User/Admin nhận thông báo hoàn tiền thành công
    REFUND_FAILED,         // User/Admin nhận thông báo hoàn tiền thất bại
    REFUND_CANCELLED,      // User nhận thông báo yêu cầu bị hủy

    // Withdrawal notifications (THÊM MỚI)
    WITHDRAWAL_REQUESTED,
    WITHDRAWAL_APPROVED,
    WITHDRAWAL_REJECTED,
    CONTRACT_LIQUIDATED,

}

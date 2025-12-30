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

    // Thông báo hệ thống
    SYSTEM_ANNOUNCEMENT,    // Thông báo hệ thống
    SYSTEM_MAINTENANCE,     // Bảo trì hệ thống

    // Thông báo khác
    GENERAL,                 // Thông báo chung

    RECONCILIATION_REQUEST_CREATED,    // Merchant tạo yêu cầu mới
    RECONCILIATION_REQUEST_APPROVED,   // Admin approve
    RECONCILIATION_REQUEST_REJECTED,   // Admin reject
    RECONCILIATION_CLAIM_SUBMITTED,    // Merchant báo cáo sai sót
}

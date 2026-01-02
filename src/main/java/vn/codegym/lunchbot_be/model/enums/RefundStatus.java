package vn.codegym.lunchbot_be.model.enums;

public enum RefundStatus {
    PENDING,      // Chờ xử lý
    PROCESSING,   // Đang xử lý
    COMPLETED,    // Đã hoàn tiền
    FAILED,       // Thất bại
    CANCELLED     // Hủy yêu cầu
}
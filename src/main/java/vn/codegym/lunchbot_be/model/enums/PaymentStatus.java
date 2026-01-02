package vn.codegym.lunchbot_be.model.enums;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    REFUND_PENDING,  // Chờ hoàn tiền
}

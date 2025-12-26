package vn.codegym.lunchbot_be.model.enums;

public enum ReconciliationStatus {
    PENDING,
    REPORTED,// Đang chờ Admin xử lý
    APPROVED,   // Admin đã duyệt
    REJECTED
}

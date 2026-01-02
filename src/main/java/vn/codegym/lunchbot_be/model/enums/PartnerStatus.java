package vn.codegym.lunchbot_be.model.enums;

public enum PartnerStatus {
    NONE,       // Chưa đăng ký
    PENDING,    // Đã gửi yêu cầu, chờ Admin duyệt
    APPROVED,   // Đã là đối tác thân thiết
    REJECTED,    // Bị từ chối
}

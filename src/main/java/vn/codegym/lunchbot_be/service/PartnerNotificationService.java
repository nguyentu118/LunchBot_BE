package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.model.Merchant;

public interface PartnerNotificationService {
    /**
     * Gửi thông báo cho admin khi merchant đăng ký đối tác thân thiết
     */
    void notifyAdminNewPartnerRequest(Merchant merchant);

    /**
     * Gửi thông báo cho merchant khi admin duyệt yêu cầu
     */
    void notifyMerchantPartnerApproved(Merchant merchant);

    /**
     * Gửi thông báo cho merchant khi admin từ chối yêu cầu
     */
    void notifyMerchantPartnerRejected(Merchant merchant, String reason);
}

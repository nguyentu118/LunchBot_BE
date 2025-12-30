package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.model.ReconciliationRequest;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;

public interface ReconciliationNotificationService {

    /**
     * Gửi thông báo cho Admin khi Merchant tạo yêu cầu đối soát mới
     */
    void notifyAdminNewReconciliationRequest(ReconciliationRequest request);

    /**
     * Gửi thông báo cho Admin khi Merchant gửi báo cáo sai sót (claim)
     */
    void notifyAdminReconciliationClaim(ReconciliationRequest request);

    /**
     * Gửi thông báo cho Merchant khi Admin approve yêu cầu đối soát
     */
    void notifyMerchantRequestApproved(ReconciliationRequest request);

    /**
     * Gửi thông báo cho Merchant khi Admin reject yêu cầu đối soát
     */
    void notifyMerchantRequestRejected(ReconciliationRequest request);

    /**
     * Gửi thông báo chung khi trạng thái yêu cầu đối soát thay đổi
     */
    void notifyReconciliationStatusChanged(
            ReconciliationRequest request,
            ReconciliationStatus oldStatus,
            ReconciliationStatus newStatus
    );

    /**
     * Gửi thông báo cho tất cả Admin về yêu cầu mới
     */
    void notifyAllAdminsNewRequest(ReconciliationRequest request);
}
// service/AdminMerchantService.java
package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.codegym.lunchbot_be.dto.request.MerchantApprovalRequest;
import vn.codegym.lunchbot_be.dto.request.MerchantLockRequest;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantListResponse;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantResponse;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

public interface AdminMerchantService {
    Page<AdminMerchantListResponse> getAllMerchants(Pageable pageable);
    Page<AdminMerchantListResponse> getMerchantsByStatus(MerchantStatus status, Pageable pageable);
    Page<AdminMerchantListResponse> searchMerchants(String keyword, Pageable pageable);
    AdminMerchantResponse getMerchantDetails(Long merchantId);
    AdminMerchantResponse approveMerchant(Long merchantId, MerchantApprovalRequest request);
    AdminMerchantResponse lockUnlockMerchant(Long merchantId, MerchantLockRequest request);
    Long countPendingMerchants();
    Long countLockedMerchants();
    Long countApprovedMerchants();
}

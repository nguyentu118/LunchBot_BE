package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.WithdrawalCreateDTO;
import vn.codegym.lunchbot_be.dto.response.WithdrawalHistoryResponse;
import vn.codegym.lunchbot_be.model.WithdrawalRequest;
import vn.codegym.lunchbot_be.model.enums.WithdrawalStatus;

import java.util.List;

public interface FinancialService {
    void createWithdrawalRequest(Long merchantId, WithdrawalCreateDTO requestDTO);
    void liquidateContract(Long merchantId, WithdrawalCreateDTO bankInfoDTO);
    List<WithdrawalHistoryResponse> getMerchantWithdrawalHistory(Long merchantId);
    // Lấy danh sách yêu cầu rút tiền (có thể lọc theo status)
    List<WithdrawalHistoryResponse> getWithdrawalRequestsByStatus(WithdrawalStatus status);
    // Duyệt yêu cầu
    void approveWithdrawal(Long requestId);
    // Từ chối yêu cầu (Kèm hoàn tiền)
    void rejectWithdrawal(Long requestId, String reason);
}
package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.WithdrawalCreateDTO;
import vn.codegym.lunchbot_be.dto.response.WithdrawalHistoryResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.WithdrawalRequest;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.model.enums.WithdrawalStatus;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.WithdrawalRequestRepository;
import vn.codegym.lunchbot_be.service.FinancialService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialServiceImpl implements FinancialService {

    private final MerchantRepository merchantRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final OrderRepository orderRepository;
    private final WithdrawalNotificationServiceImpl withdrawalNotificationService; // ✅ INJECT INTERFACE

    // --- TASK 24: RÚT TIỀN THÔNG THƯỜNG ---
    @Override
    @Transactional
    public void createWithdrawalRequest(Long merchantId, WithdrawalCreateDTO requestDTO) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));

        BigDecimal amount = requestDTO.getAmount();

        // 1. Kiểm tra số dư
        if (merchant.getCurrentBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Số dư hiện tại (" + merchant.getCurrentBalance() + ") không đủ để rút " + amount);
        }

        // 2. Trừ tiền ngay lập tức (Đóng băng)
        merchant.setCurrentBalance(merchant.getCurrentBalance().subtract(amount));

        // 3. Cập nhật thông tin ngân hàng (để lần sau tự điền)
        updateBankInfo(merchant, requestDTO);
        merchantRepository.save(merchant);

        // 4. Tạo yêu cầu
        WithdrawalRequest request = WithdrawalRequest.builder()
                .merchant(merchant)
                .amount(amount)
                .status(WithdrawalStatus.PENDING)
                .adminNotes("Yêu cầu rút tiền thường")
                .build();
        WithdrawalRequest savedRequest = withdrawalRequestRepository.save(request); // ✅ LƯU LẠI

        // ✅ GỬI THÔNG BÁO
        try {
            withdrawalNotificationService.notifyMerchantWithdrawalRequested(savedRequest);
            withdrawalNotificationService.notifyAdminNewWithdrawalRequest(savedRequest);
        } catch (Exception e) {
            log.error("❌ Failed to send withdrawal notifications", e);
        }
    }

    // --- TASK 23: THANH LÝ HỢP ĐỒNG ---
    @Override
    @Transactional
    public void liquidateContract(Long merchantId, WithdrawalCreateDTO bankInfoDTO) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant không tồn tại"));

        // 1. Kiểm tra điều kiện doanh thu tháng > 100tr
        BigDecimal currentMonthRevenue = calculateCurrentMonthRevenue(merchantId);
        if (currentMonthRevenue.compareTo(new BigDecimal("100000000")) <= 0) {
            throw new IllegalStateException("Điều kiện thanh lý không thỏa mãn: Doanh thu tháng hiện tại phải > 100 triệu (Hiện tại: " + currentMonthRevenue + ")");
        }

        // 2. Kiểm tra đơn hàng tồn động
        boolean hasActiveOrders = orderRepository.existsByMerchantIdAndStatusIn(
                merchantId,
                Arrays.asList(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.READY, OrderStatus.DELIVERING)
        );
        if (hasActiveOrders) {
            throw new IllegalStateException("Không thể thanh lý: Vẫn còn đơn hàng đang xử lý. Vui lòng hoàn thành hoặc hủy hết đơn hàng.");
        }

        // 3. Rút TOÀN BỘ tiền còn lại
        BigDecimal remainingBalance = merchant.getCurrentBalance();
        WithdrawalRequest savedRequest = null; // ✅ KHAI BÁO NGOÀI IF

        if (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {
            merchant.setCurrentBalance(BigDecimal.ZERO); // Trừ sạch ví

            WithdrawalRequest request = WithdrawalRequest.builder()
                    .merchant(merchant)
                    .amount(remainingBalance)
                    .status(WithdrawalStatus.PENDING)
                    .adminNotes("THANH LÝ HỢP ĐỒNG - RÚT TOÀN BỘ")
                    .build();
            savedRequest = withdrawalRequestRepository.save(request); // ✅ LƯU LẠI
        }

        // 4. Khóa tài khoản vĩnh viễn
        merchant.setStatus(MerchantStatus.LOCKED);
        merchant.setIsLocked(true);
        merchant.setRejectionReason("Đã yêu cầu thanh lý hợp đồng");
        updateBankInfo(merchant, bankInfoDTO);
        merchantRepository.save(merchant);

        // ✅ GỬI THÔNG BÁO (chỉ khi có withdrawal request)
        if (savedRequest != null) {
            try {
                withdrawalNotificationService.notifyMerchantContractLiquidated(savedRequest);
                withdrawalNotificationService.notifyAdminContractLiquidation(savedRequest);
            } catch (Exception e) {
                log.error("❌ Failed to send liquidation notifications", e);
            }
        }
    }

    @Override
    public List<WithdrawalHistoryResponse> getMerchantWithdrawalHistory(Long merchantId) {
        List<WithdrawalRequest> requests = withdrawalRequestRepository.findByMerchantId(merchantId);

        return requests.stream()
                .map(req -> WithdrawalHistoryResponse.builder()
                        .id(req.getId())
                        .amount(req.getAmount())
                        .requestedAt(req.getRequestedAt())
                        .status(req.getStatus())
                        .adminNotes(req.getAdminNotes())
                        .merchant(WithdrawalHistoryResponse.MerchantBankInfo.builder()
                                .bankName(req.getMerchant().getBankName())
                                .bankAccountNumber(req.getMerchant().getBankAccountNumber())
                                .bankAccountHolder(req.getMerchant().getBankAccountHolder())
                                .restaurantName(req.getMerchant().getRestaurantName())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<WithdrawalHistoryResponse> getWithdrawalRequestsByStatus(WithdrawalStatus status) {
        List<WithdrawalRequest> requests;
        if (status == null) {
            requests = withdrawalRequestRepository.findAll();
        } else {
            requests = withdrawalRequestRepository.findByStatus(status);
        }

        return requests.stream()
                .map(req -> WithdrawalHistoryResponse.builder()
                        .id(req.getId())
                        .amount(req.getAmount())
                        .requestedAt(req.getRequestedAt())
                        .status(req.getStatus())
                        .adminNotes(req.getAdminNotes())
                        .merchant(WithdrawalHistoryResponse.MerchantBankInfo.builder()
                                .bankName(req.getMerchant().getBankName())
                                .bankAccountNumber(req.getMerchant().getBankAccountNumber())
                                .bankAccountHolder(req.getMerchant().getBankAccountHolder())
                                .restaurantName(req.getMerchant().getRestaurantName())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void approveWithdrawal(Long requestId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể duyệt các yêu cầu đang chờ (PENDING).");
        }

        // 1. Cập nhật trạng thái
        request.approve("Admin đã duyệt chuyển khoản.");
        WithdrawalRequest saved = withdrawalRequestRepository.save(request);

        // ✅ GỬI THÔNG BÁO
        try {
            withdrawalNotificationService.notifyMerchantWithdrawalApproved(saved);
        } catch (Exception e) {
            log.error("❌ Failed to send approval notification", e);
        }
    }

    @Override
    @Transactional
    public void rejectWithdrawal(Long requestId, String reason) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));

        if (request.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể từ chối các yêu cầu đang chờ (PENDING).");
        }

        // 1. Cập nhật trạng thái Request
        request.reject(reason);
        WithdrawalRequest saved = withdrawalRequestRepository.save(request);

        // 2. QUAN TRỌNG: HOÀN TIỀN VỀ VÍ MERCHANT
        Merchant merchant = request.getMerchant();
        BigDecimal refundAmount = request.getAmount();
        merchant.setCurrentBalance(merchant.getCurrentBalance().add(refundAmount));
        merchantRepository.save(merchant);

        // ✅ GỬI THÔNG BÁO
        try {
            withdrawalNotificationService.notifyMerchantWithdrawalRejected(saved);
        } catch (Exception e) {
            log.error("❌ Failed to send rejection notification", e);
        }
    }

    // Helper: Cập nhật thông tin bank
    private void updateBankInfo(Merchant merchant, WithdrawalCreateDTO dto) {
        merchant.setBankName(dto.getBankName());
        merchant.setBankAccountNumber(dto.getBankAccountNumber());
        merchant.setBankAccountHolder(dto.getBankAccountHolder());
    }

    // Helper: Tính doanh thu tháng hiện tại
    private BigDecimal calculateCurrentMonthRevenue(Long merchantId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        BigDecimal revenue = orderRepository.sumRevenueByMerchantAndDateRange(merchantId, startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}
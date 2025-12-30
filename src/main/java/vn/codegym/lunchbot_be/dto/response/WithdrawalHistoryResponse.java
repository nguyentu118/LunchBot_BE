package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalHistoryResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime requestedAt;
    private WithdrawalStatus status;
    private String adminNotes;

    // Object lồng nhau để khớp với Frontend: item.merchant.bankName
    private MerchantBankInfo merchant;

    @Data
    @Builder
    public static class MerchantBankInfo {
        private String bankName;
        private String bankAccountNumber;
        private String bankAccountHolder;
        private String restaurantName;
    }
}
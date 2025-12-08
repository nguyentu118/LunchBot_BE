// dto/request/MerchantApprovalRequest.java
package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantApprovalRequest {
    @NotNull(message = "Approval status is required")
    private Boolean approved; // true = duyệt, false = từ chối

    private String reason; // Lý do
}






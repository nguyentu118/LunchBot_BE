package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantLockRequest {
    @NotNull(message = "Lock status is required")
    private Boolean lock; // true = khóa, false = mở khóa

    @NotBlank(message = "Reason is required")
    private String reason;
}


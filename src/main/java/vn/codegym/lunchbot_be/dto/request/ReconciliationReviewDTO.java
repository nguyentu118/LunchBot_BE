package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationReviewDTO {

    @NotBlank(message = "Lý do từ chối không được để trống")
    private String rejectionReason;

    private String adminNotes; // Ghi chú từ admin (optional)
}
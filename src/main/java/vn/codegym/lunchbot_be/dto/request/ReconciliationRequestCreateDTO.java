package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRequestCreateDTO {

    @NotBlank(message = "Tháng đối soát không được để trống")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Định dạng tháng phải là YYYY-MM (VD: 2025-12)")
    private String yearMonth;

    private String merchantNotes; // Ghi chú từ merchant (optional)
}
package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReconciliationClaimDTO {

    @NotBlank(message = "Tháng đối soát không được để trống")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Định dạng tháng phải là YYYY-MM")
    private String yearMonth;

    @NotBlank(message = "Vui lòng nhập lý do sai sót/khiếu nại")
    private String reason; // Đây chính là merchantNotes nhưng bắt buộc

    // Có thể mở rộng thêm: List<Long> disputedOrderIds (Danh sách ID đơn hàng bị sai) nếu cần chi tiết
}
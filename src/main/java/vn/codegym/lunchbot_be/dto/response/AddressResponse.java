package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String contactName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String building;
    private Boolean isDefault;

    // Địa chỉ đầy đủ (ghép tất cả)
    private String fullAddress;

    // Loại địa chỉ (để hiển thị)
    private String addressType; // "Nhà riêng", "Văn phòng", "Mặc định"

    /**
     * Helper method để tạo fullAddress
     */
    public String buildFullAddress() {
        StringBuilder sb = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            sb.append(street);
        }

        if (building != null && !building.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(building);
        }

        if (ward != null && !ward.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(ward);
        }

        if (district != null && !district.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(district);
        }

        if (province != null && !province.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(province);
        }

        return sb.toString();
    }
}
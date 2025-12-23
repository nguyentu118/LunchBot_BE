package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder; // 💡 THÊM IMPORT
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.Merchant; // 💡 THÊM IMPORT

@Data
@Builder // 💡 THÊM BUILDER
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseDTO {
    private Long id; // 💡 THÊM TRƯỜNG ID
    private String restaurantName;
    private String avatarUrl;
    private String address;
    private String email;
    private String phone;
    private String openTime;
    private String closeTime;

    // 💡 THÊM PHƯƠNG THỨC CHUYỂN ĐỔI TỪ ENTITY
    public static MerchantResponseDTO fromEntity(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        return MerchantResponseDTO.builder()
                .id(merchant.getId())
                .avatarUrl(merchant.getAvatarUrl())
                .restaurantName(merchant.getRestaurantName()) // Giả định Merchant.name = restaurantName
                .address(merchant.getAddress())
                .phone(merchant.getPhone())
                .build();
    }
}
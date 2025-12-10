package vn.codegym.lunchbot_be.dto.response;

import lombok.Data;

@Data
public class MerchantResponseDTO {
    private String restaurantName;
    private String address;
    private String email;
    private String phone;
    private String openTime;
    private String closeTime;
}

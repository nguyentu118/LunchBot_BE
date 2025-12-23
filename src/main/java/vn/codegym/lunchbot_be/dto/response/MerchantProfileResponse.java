package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class MerchantProfileResponse {
    private String restaurantName;
    private String phone;
    private String address;
    private String avatarUrl;
    private LocalTime openTime;
    private LocalTime closeTime;
}

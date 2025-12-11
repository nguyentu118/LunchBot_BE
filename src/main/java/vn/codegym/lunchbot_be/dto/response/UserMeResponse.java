package vn.codegym.lunchbot_be.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserMeResponse {
    private String fullName;
    private Boolean isLoggedIn = true;// luôn trả về true khi gọi API này
}

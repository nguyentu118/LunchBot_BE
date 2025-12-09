package vn.codegym.lunchbot_be.dto.response;

import lombok.Data;
import vn.codegym.lunchbot_be.model.enums.Gender;

import java.time.LocalDate;

@Data
public class UserResponseDTO {
    private String email;
    private String phone;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String shippingAddress;
}

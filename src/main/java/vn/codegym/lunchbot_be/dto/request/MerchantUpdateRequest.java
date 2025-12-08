package vn.codegym.lunchbot_be.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

public class MerchantUpdateRequest {

    @NotBlank(message = "Tên nhà hàng không được để trống")
    private String restaurantName;


    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @NotBlank(message = "Giờ mở cửa không được để trống")
    private String openTime;

    @NotBlank(message = "Giờ đóng cửa không được để trống")
    private String closeTime;

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }


    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }
}

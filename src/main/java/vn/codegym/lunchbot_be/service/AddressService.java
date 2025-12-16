package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.AddressRequest;
import vn.codegym.lunchbot_be.dto.response.AddressResponse;

import java.util.List;

/**
 * Service quản lý địa chỉ giao hàng của người dùng
 */
public interface AddressService {

    /**
     * Lấy tất cả địa chỉ của user
     */
    List<AddressResponse> getAllAddressesByUser(String email);

    /**
     * Lấy thông tin một địa chỉ cụ thể
     */
    AddressResponse getAddressById(String email, Long addressId);

    /**
     * Tạo địa chỉ mới
     */
    AddressResponse createAddress(String email, AddressRequest request);

    /**
     * Cập nhật địa chỉ
     */
    AddressResponse updateAddress(String email, Long addressId, AddressRequest request);

    /**
     * Xóa địa chỉ

     */
    void deleteAddress(String email, Long addressId);

    /**
     * Đặt địa chỉ làm mặc định
     */
    AddressResponse setDefaultAddress(String email, Long addressId);

    /**
     * Lấy địa chỉ mặc định của user
     */
    AddressResponse getDefaultAddress(String email);
}
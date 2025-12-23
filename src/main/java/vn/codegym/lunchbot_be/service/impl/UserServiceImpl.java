package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.UserUpdateDTO;
import vn.codegym.lunchbot_be.dto.response.UserMeResponse;
import vn.codegym.lunchbot_be.dto.response.UserResponseDTO;
import vn.codegym.lunchbot_be.model.Address;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.AddressRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {
    private final UserRepository userRepository;

    private final AddressRepository addressRepository;

    @Transactional
    public void updateProfile(String currentEmail, UserUpdateDTO updateRequest) {

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User không tồn tại: " + currentEmail));

        // 1. Cập nhật các trường cá nhân
        user.setFullName(updateRequest.getFullName());
        user.setDateOfBirth(updateRequest.getDateOfBirth());
        user.setGender(updateRequest.getGender());

        // 2. Xử lý Địa chỉ giao hàng (Cập nhật địa chỉ mặc định/đầu tiên)
        if (updateRequest.getShippingAddress() != null && !updateRequest.getShippingAddress().trim().isEmpty()) {
            updateDefaultAddress(user, updateRequest.getShippingAddress());
        }

        // 3. Lưu User
        userRepository.save(user);
    }

    // Logic phụ: Tìm hoặc tạo địa chỉ để cập nhật
    private void updateDefaultAddress(User user, String newAddressDetail) {

        Optional<Address> defaultAddressOpt = user.getAddresses().stream()
                .filter(Address::getIsDefault)
                .findFirst();

        Address addressToUpdate;
        boolean isNewAddress = false;

        if (defaultAddressOpt.isPresent()) {
            // Trường hợp 1: Địa chỉ mặc định đã tồn tại -> Cập nhật
            addressToUpdate = defaultAddressOpt.get();
        } else if (!user.getAddresses().isEmpty()) {
            // Trường hợp 2: Không có mặc định, sử dụng địa chỉ đầu tiên
            addressToUpdate = user.getAddresses().get(0);
        } else {
            // Trường hợp 3: Chưa có địa chỉ nào -> Tạo mới
            isNewAddress = true;
            addressToUpdate = Address.builder()
                    .user(user)
                    .isDefault(true)
                    // Khởi tạo các trường BẮT BUỘC trong Address Entity
                    .contactName(user.getFullName() != null ? user.getFullName() : "Khách hàng")
                    .phone(user.getPhone() != null ? user.getPhone() : "0000000000")
                    .province("Chưa phân loại")
                    .district("Chưa phân loại")
                    .ward("Chưa phân loại")
                    .street("") // Sẽ được cập nhật ngay sau đó
                    .build();
            user.getAddresses().add(addressToUpdate);
        }

        // 2. Cập nhật Chi tiết Địa chỉ bằng trường 'street'
        // KHẮC PHỤC LỖI: setStreet() thay vì setDetail()
        addressToUpdate.setStreet(newAddressDetail);

        // 3. Cập nhật contactName và phone theo thông tin mới nhất của User (nếu cần)
        if (!isNewAddress) {
            addressToUpdate.setContactName(user.getFullName() != null ? user.getFullName() : "Khách hàng");
            addressToUpdate.setPhone(user.getPhone() != null ? user.getPhone() : "0000000000");
        }

        addressRepository.save(addressToUpdate);
    }


    @Transactional(readOnly = true)
    public UserResponseDTO getProfile(String currentEmail) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User không tồn tại: " + currentEmail));

        // Lấy địa chỉ mặc định
        String defaultAddress = user.getAddresses().stream()
                .filter(Address::getIsDefault)
                .findFirst()
                // KHẮC PHỤC LỖI: getStreet() thay vì getDetail()
                .map(Address::getStreet)
                .orElse(
                        user.getAddresses().isEmpty() ?
                                "" :
                                user.getAddresses().get(0).getStreet() // Lấy địa chỉ đầu tiên nếu không có mặc định
                );


        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .shippingAddress(defaultAddress)
                .build();
    }
    // Thêm phương thức mới để chỉ lấy thông tin cần cho Header
    public UserMeResponse getHeaderUserInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 2. Map sang DTO
        return UserMeResponse.builder()
                .fullName(user.getFullName()) // Lấy tên hiển thị từ Entity
                .isLoggedIn(true)
                .build();
    }
}

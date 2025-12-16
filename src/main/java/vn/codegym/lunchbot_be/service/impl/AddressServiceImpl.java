package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.AddressRequest;
import vn.codegym.lunchbot_be.dto.response.AddressResponse;
import vn.codegym.lunchbot_be.model.Address;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.AddressRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.AddressService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddressesByUser(String email) {// lay dia chi cua user qua email
        User user = getUserByEmail(email);

        List<Address> addresses = addressRepository.findByUserId(user.getId());

        return addresses.stream()
                .map(this::mapToResponse)// chuyen dia chi entity sang dto
                .collect(Collectors.toList());// thu thap thanh lis
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(String email, Long addressId) {
        User user = getUserByEmail(email);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Kiểm tra địa chỉ có thuộc về user không
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập địa chỉ này");
        }

        return mapToResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse createAddress(String email, AddressRequest request) {
        User user = getUserByEmail(email);

        // Nếu đây là địa chỉ đầu tiên, tự động set làm mặc định
        Long addressCount = addressRepository.countByUserId(user.getId());
        boolean shouldBeDefault = (addressCount == 0) ||
                (request.getIsDefault() != null && request.getIsDefault());

        // Nếu set làm mặc định, bỏ default của địa chỉ khác
        if (shouldBeDefault) {
            unsetOtherDefaultAddresses(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .contactName(request.getContactName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .building(request.getBuilding())
                .isDefault(shouldBeDefault)
                .build();

        Address savedAddress = addressRepository.save(address);

        return mapToResponse(savedAddress);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(String email, Long addressId, AddressRequest request) {
        User user = getUserByEmail(email);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Kiểm tra quyền sở hữu
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật địa chỉ này");
        }

        // Cập nhật thông tin
        address.setContactName(request.getContactName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setStreet(request.getStreet());
        address.setBuilding(request.getBuilding());

        // Xử lý set default
        if (request.getIsDefault() != null && request.getIsDefault()) {
            unsetOtherDefaultAddresses(user.getId());
            address.setIsDefault(true);
        }

        Address updatedAddress = addressRepository.save(address);

        return mapToResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = getUserByEmail(email);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Kiểm tra quyền sở hữu
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này");
        }

        boolean wasDefault = address.getIsDefault();

        addressRepository.delete(address);

        // Nếu xóa địa chỉ mặc định, tự động set địa chỉ khác làm mặc định
        if (wasDefault) {
            List<Address> remainingAddresses = addressRepository.findByUserId(user.getId());
            if (!remainingAddresses.isEmpty()) {
                Address firstAddress = remainingAddresses.get(0);
                firstAddress.setIsDefault(true);
                addressRepository.save(firstAddress);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(String email, Long addressId) {
        User user = getUserByEmail(email);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ"));

        // Kiểm tra quyền sở hữu
        if (!address.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập địa chỉ này");
        }

        // Bỏ default của các địa chỉ khác
        unsetOtherDefaultAddresses(user.getId());

        // Set địa chỉ này làm mặc định
        address.setIsDefault(true);
        Address updatedAddress = addressRepository.save(address);

        return mapToResponse(updatedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(String email) {
        User user = getUserByEmail(email);

        return addressRepository.findByUserIdAndIsDefault(user.getId(), true)
                .map(this::mapToResponse)
                .orElse(null);
    }

    // ========== HELPER METHODS ==========

    /**
     * Lấy User từ email
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    /**
     * Bỏ default của tất cả địa chỉ khác
     */
    private void unsetOtherDefaultAddresses(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        addresses.forEach(addr -> {
            if (addr.getIsDefault()) {
                addr.setIsDefault(false);
                addressRepository.save(addr);
            }
        });
    }

    /**
     * Map Address entity sang AddressResponse DTO
     */
    private AddressResponse mapToResponse(Address address) {
        AddressResponse response = AddressResponse.builder()
                .id(address.getId())
                .contactName(address.getContactName())
                .phone(address.getPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .street(address.getStreet())
                .building(address.getBuilding())
                .isDefault(address.getIsDefault())
                .build();

        // Tạo fullAddress
        response.setFullAddress(response.buildFullAddress());

        // Xác định loại địa chỉ
        if (address.getIsDefault()) {
            response.setAddressType("Mặc định");
        } else if (address.getBuilding() != null &&
                address.getBuilding().toLowerCase().contains("công ty")) {
            response.setAddressType("Văn phòng");
        } else {
            response.setAddressType("Nhà riêng");
        }

        return response;
    }
}
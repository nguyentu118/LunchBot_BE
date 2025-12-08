package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.UserRole;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;


@Service
@RequiredArgsConstructor
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailServiceImpl emailService; // Cần tạo EmailService

    @Transactional
    public User registerMerchant(MerchantRegisterRequest request) {
        // 1. Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng.");
        }

        // 2. Tạo User mới
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(UserRole.MERCHANT)
                .isActive(true)
                .isEmailVerified(false)
                .build();

        user = userRepository.save(user);


        Merchant merchant = Merchant.builder()
                .restaurantName(request.getRestaurantName() != null && !request.getRestaurantName().isEmpty()
                        ? request.getRestaurantName()
                        : request.getEmail()) // Dùng email nếu tên nhà hàng trống
                .address(request.getAddress())
                .user(user)
                .build();

        merchantRepository.save(merchant);


        user.setMerchant(merchant);


        try {
            String subject = "Đăng ký Merchant thành công!";
            String body = String.format("Chào mừng bạn %s, bạn đã đăng ký thành công làm Merchant. Thông tin nhà hàng: %s",
                    request.getEmail(), merchant.getRestaurantName());
            emailService.sendRegistrationSuccessEmail(user.getEmail(), null, merchant.getRestaurantName(),
                    "http://localhost:5173/register-merchant"); // Thay URL đăng nhập thực tế

        } catch (Exception e) {
            // Xử lý lỗi gửi email (ví dụ: log lỗi)
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }

        return user;
    }
}

package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.dto.response.AuthResponse;
import vn.codegym.lunchbot_be.dto.request.LoginRequest;
import vn.codegym.lunchbot_be.dto.request.RegistrationRequest;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
import vn.codegym.lunchbot_be.model.enums.UserRole;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.EmailService;
import vn.codegym.lunchbot_be.util.JwtUtil;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;

    private final MerchantRepository merchantRepository;

    private final PasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final JwtUtil jwtUtil;

    private final AuthenticationManager authenticationManager;

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
                .phone(request.getPhone())
                .user(user)
                .isApproved(false)
                .isPartner(false)
                .isLocked(false)
                .status(MerchantStatus.PENDING)
                .build();

        merchantRepository.save(merchant);


        user.setMerchant(merchant);


        try {
            // Khắc phục lỗi: Đảm bảo fullName KHÔNG NULL
            String recipientName = user.getFullName() != null
                    ? user.getFullName()
                    : user.getEmail();


            String merchantName = merchant.getRestaurantName() != null
                    ? merchant.getRestaurantName()
                    : "";

            emailService.sendRegistrationSuccessEmail(
                    user.getEmail(),
                    recipientName,
                    merchantName,
                    "http://localhost:5173/login",
                    true
            );


        } catch (Exception e) {
            // Xử lý lỗi gửi email (ví dụ: log lỗi)
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }

        return user;
    }

    @Transactional
    public User registerUser(RegistrationRequest request) {
        // 1. Kiểm tra Mật khẩu và Xác nhận Mật khẩu
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp.");
        }

        // 2. Kiểm tra Email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã được đăng ký. Vui lòng sử dụng email khác.");
        }

        // 3. Tạo User Entity
        User newUser = User.builder()
                .email(request.getEmail())
                .fullName(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER) // Đảm bảo role là USER
                .isActive(true)
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(newUser);

        // 4. Gửi Email thông báo (Theo yêu cầu của Task)
        try {
            // 1. Đảm bảo tên người nhận KHÔNG NULL
            String recipientName = savedUser.getFullName() != null
                    ? savedUser.getFullName()
                    : savedUser.getEmail();

            // 2. Đảm bảo MerchantName KHÔNG NULL (vì User thường không có MerchantName)
            // Lỗi xảy ra là do tham số này!
            String merchantName = ""; // <--- SỬ DỤNG CHUỖI RỖNG AN TOÀN

            emailService.sendRegistrationSuccessEmail(
                    savedUser.getEmail(),
                    recipientName,
                    merchantName,  // <--- ĐÃ THAY NULL BẰNG CHUỖI RỖNG
                    "http://localhost:5173/login",
                    false
            );
        } catch (Exception e) {
            System.err.println("Lỗi gửi email: " + e.getMessage());
        }

        return savedUser;
    }

    public AuthResponse login(LoginRequest request) {

        // Bước này sẽ tự động gọi CustomUserDetailsService.loadUserByUsername()
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        //Lưu thông tin vào Security Context (Tùy chọn, nhưng nên làm)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy UserDetails object đã được xác thực (là đối tượng User của bạn)
        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        // Lấy lại User object từ DB để lấy các thông tin khác (Merchant, Role...)
        // Hoặc sử dụng method getUserByEmail của CustomUserDetailsService nếu bạn có
        User user = userRepository.findByEmail(springUser.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found after successful authentication"));

        // Tạo JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
        // Trả về Response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .userId(user.getId())
                .build();
    }

}

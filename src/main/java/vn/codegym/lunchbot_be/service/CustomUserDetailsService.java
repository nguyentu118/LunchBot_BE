package vn.codegym.lunchbot_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException; // Đảm bảo import
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus; // Đảm bảo import
import vn.codegym.lunchbot_be.repository.UserRepository;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));

        // 2. 🔑 LOGIC CHẶN ĐĂNG NHẬP MERCHANT NẾU CHƯA ĐƯỢC DUYỆT (PENDING, REJECTED, LOCKED)
        if (user.getMerchant() != null) {
            MerchantStatus status = user.getMerchant().getStatus();

            // ⭐ ĐIỀU KIỆN QUAN TRỌNG: Chỉ cho phép đăng nhập nếu trạng thái là APPROVED
            if (status != MerchantStatus.APPROVED) {
                String message;
                switch (status) {
                    case PENDING:
                        message = "Tài khoản Merchant đang chờ duyệt. Vui lòng đợi quản trị viên phê duyệt.";
                        break;
                    case REJECTED:
                        message = "Tài khoản Merchant đã bị từ chối. Vui lòng liên hệ quản trị viên.";
                        break;
                    case LOCKED:
                        // Dù bạn có trường isLocked, việc kiểm tra Enum LOCKED vẫn là cách tốt nhất
                        message = "Tài khoản Merchant đã bị khóa. Vui lòng liên hệ quản trị viên.";
                        break;
                    default:
                        message = "Tài khoản Merchant không ở trạng thái hoạt động.";
                        break;
                }

                // ⭐ Đây là điểm chặn quá trình xác thực
                throw new DisabledException(message);
            }
        }
        // 1. Kiểm tra trạng thái isActive (vô hiệu hóa chung)
        if (!user.getIsActive()) {
            throw new DisabledException("Tài khoản đã bị vô hiệu hóa");
        }

        // Nếu tất cả kiểm tra đều qua, tạo UserDetails
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user với email: " + email));
    }
}
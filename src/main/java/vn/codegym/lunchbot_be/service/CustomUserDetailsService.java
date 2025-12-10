package vn.codegym.lunchbot_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
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


        // 2. 🔑 LOGIC CHẶN ĐĂNG NHẬP MERCHANT ĐÃ BỊ KHÓA (TASK 28)
        if (user.getMerchant() != null) {
            // Chỉ cho phép đăng nhập nếu trạng thái là APPROVED
            if (user.getMerchant().getStatus() != MerchantStatus.APPROVED) {
                String message;
                switch (user.getMerchant().getStatus()) {
                    case PENDING:
                        message = "Tài khoản Merchant đang ở trạng thái Chờ duyệt. Vui lòng đợi quản trị viên phê duyệt.";
                        break;
                    case REJECTED:
                        message = "Tài khoản Merchant đã bị Từ chối. Vui lòng liên hệ quản trị viên.";
                        break;
                    case LOCKED:
                        message = "Tài khoản Merchant đã bị Khóa. Vui lòng liên hệ quản trị viên.";
                        break;
                    default:
                        message = "Tài khoản Merchant chưa được phê duyệt và không thể đăng nhập.";
                        break;
                }
                // Ném ra DisabledException để Spring Security chặn quá trình xác thực
                throw new DisabledException(message);
            }
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

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
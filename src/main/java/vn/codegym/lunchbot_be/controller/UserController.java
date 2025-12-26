package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.UserUpdateDTO;
import vn.codegym.lunchbot_be.dto.response.UserMeResponse;
import vn.codegym.lunchbot_be.dto.response.UserResponseDTO;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.impl.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserServiceImpl userService;

    private final UserRepository userRepository;

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            // Ném 401 Unauthorized nếu không tìm thấy token hợp lệ
            throw new SecurityException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        // Lấy tên Principal, thường là Email/Username khi sử dụng JWT
        return authentication.getName();
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            String email = getCurrentUserEmail();
            UserResponseDTO profile = userService.getProfile(email);
            return ResponseEntity.ok(profile);
        } catch (SecurityException e) {
            // Lỗi xác thực
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            // Lỗi nghiệp vụ (ví dụ: User không tồn tại)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy hồ sơ cá nhân: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UserUpdateDTO updateRequest) {
        try {
            String email = getCurrentUserEmail();
            userService.updateProfile(email, updateRequest);
            return ResponseEntity.ok("Cập nhật thông tin cá nhân thành công!");
        } catch (SecurityException e) {
            // Lỗi xác thực
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            // Lỗi Validation (@Valid) hoặc lỗi nghiệp vụ (UserService)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cập nhật thất bại: " + e.getMessage());
        }
    }
    @GetMapping("/me")
    public ResponseEntity<?> getMeInfo() {
        try {
            String email = getCurrentUserEmail();
            UserMeResponse userInfo = userService.getHeaderUserInfo(email);
            return ResponseEntity.ok(userInfo);
        } catch (SecurityException e) {
            // Lỗi xác thực: Trả về trạng thái chưa đăng nhập
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        } catch (RuntimeException e) {
            // Lỗi nghiệp vụ (User không tồn tại)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // Lấy email từ JWT token (Spring Security tự động parse)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        // Tìm user trong database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Trả về thông tin cần thiết
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole());

        return ResponseEntity.ok(response);
    }

}

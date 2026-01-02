package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.UpdateBankInfoRequest;
import vn.codegym.lunchbot_be.dto.request.UserUpdateDTO;
import vn.codegym.lunchbot_be.dto.response.UserMeResponse;
import vn.codegym.lunchbot_be.dto.response.UserResponseDTO;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.impl.UserServiceImpl;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
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
    /**
     * Lấy thông tin ngân hàng hiện tại
     * GET /api/users/bank-info
     */
    @GetMapping("/bank-info")
    public ResponseEntity<Map<String, Object>> getBankInfo() {
        try {
            String email = getCurrentUserEmail();
            log.info("💳 User {} getting bank info", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Map<String, Object> bankInfo = new HashMap<>();
            bankInfo.put("bankAccountNumber", user.getBankAccountNumber());
            bankInfo.put("bankName", user.getBankName());
            bankInfo.put("bankAccountName", user.getBankAccountName());
            bankInfo.put("bankBranch", user.getBankBranch());
            bankInfo.put("hasBankInfo", user.getBankAccountNumber() != null);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", bankInfo
            ));

        } catch (SecurityException e) {
            log.error("❌ Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error getting bank info: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không thể lấy thông tin ngân hàng: " + e.getMessage()
            ));
        }
    }

    /**
     * Cập nhật thông tin ngân hàng
     * PUT /api/users/bank-info
     *
     * Body: {
     *   "bankAccountNumber": "1234567890",
     *   "bankName": "Vietcombank",
     *   "bankAccountName": "NGUYEN VAN A",
     *   "bankBranch": "CN Hà Nội"
     * }
     */
    @PutMapping("/bank-info")
    public ResponseEntity<Map<String, Object>> updateBankInfo(
            @Valid @RequestBody UpdateBankInfoRequest request
    ) {
        try {
            String email = getCurrentUserEmail();
            log.info("💳 User {} updating bank info", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Cập nhật thông tin ngân hàng
            user.setBankAccountNumber(request.getBankAccountNumber());
            user.setBankName(request.getBankName());
            user.setBankAccountName(request.getBankAccountName());
            user.setBankBranch(request.getBankBranch());

            userRepository.save(user);

            log.info("✅ Bank info updated successfully for user: {}", email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cập nhật thông tin ngân hàng thành công",
                    "data", Map.of(
                            "bankAccountNumber", user.getBankAccountNumber(),
                            "bankName", user.getBankName(),
                            "bankAccountName", user.getBankAccountName(),
                            "bankBranch", user.getBankBranch() != null ? user.getBankBranch() : ""
                    )
            ));

        } catch (SecurityException e) {
            log.error("❌ Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error updating bank info: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không thể cập nhật thông tin ngân hàng: " + e.getMessage()
            ));
        }
    }

    /**
     * Xóa thông tin ngân hàng
     * DELETE /api/users/bank-info
     */
    @DeleteMapping("/bank-info")
    public ResponseEntity<Map<String, Object>> deleteBankInfo() {
        try {
            String email = getCurrentUserEmail();
            log.info("🗑️ User {} deleting bank info", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Xóa thông tin ngân hàng
            user.setBankAccountNumber(null);
            user.setBankName(null);
            user.setBankAccountName(null);
            user.setBankBranch(null);

            userRepository.save(user);

            log.info("✅ Bank info deleted successfully for user: {}", email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xóa thông tin ngân hàng"
            ));

        } catch (SecurityException e) {
            log.error("❌ Unauthorized: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting bank info: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không thể xóa thông tin ngân hàng: " + e.getMessage()
            ));
        }
    }

}

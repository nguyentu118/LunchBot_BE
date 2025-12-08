package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.dto.request.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.dto.response.AuthResponse;
import vn.codegym.lunchbot_be.dto.request.LoginRequest;
import vn.codegym.lunchbot_be.dto.request.RegistrationRequest;
import vn.codegym.lunchbot_be.service.impl.AuthServiceImpl;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;

    @PostMapping("/register/merchant")
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody MerchantRegisterRequest request,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Trích xuất và trả về lỗi chi tiết
            String errorMsg = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            // Trả về 400 kèm theo thông báo lỗi chi tiết
            return ResponseEntity.badRequest().body("Lỗi dữ liệu đầu vào: " + errorMsg);
        }
        try {
            authService.registerMerchant(request);
            // Có thể trả về JWT Token hoặc chỉ trả về thông báo thành công
            return ResponseEntity.ok("Đăng ký Merchant thành công! Vui lòng chờ duyệt.");
        } catch (RuntimeException e) {
            // Xử lý lỗi (ví dụ: email đã tồn tại)
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Xử lý các lỗi khác
            return ResponseEntity.internalServerError().body("Đăng ký thất bại do lỗi hệ thống.");
        }
    }

    @PostMapping("/register") // Hoặc /register/user
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationRequest request,
                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // Xử lý lỗi validation DTO (giống như Merchant)
            String errorMsg = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.badRequest().body("Lỗi dữ liệu đầu vào: " + errorMsg);
        }
        try {
            // Gọi phương thức service đăng ký USER thường
            authService.registerUser(request);

            return ResponseEntity.ok("Đăng ký tài khoản thành công! Vui lòng kiểm tra email.");

        } catch (IllegalArgumentException e) {
            // Bắt lỗi nghiệp vụ: Mật khẩu không khớp
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            // Bắt lỗi nghiệp vụ: Email đã tồn tại (nếu Service ném ra IllegalStateException)
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); // 409 Conflict
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Đăng ký thất bại do lỗi hệ thống.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

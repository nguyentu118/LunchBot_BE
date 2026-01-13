package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.dto.response.AuthResponse;
import vn.codegym.lunchbot_be.dto.request.LoginRequest;
import vn.codegym.lunchbot_be.dto.request.RegistrationRequest;
import vn.codegym.lunchbot_be.service.impl.AuthServiceImpl;

import java.util.stream.Collectors;
@CrossOrigin(origins = {
        "http://localhost:5173",
        "https://lunch-bot-fe.vercel.app"
})
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

    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyAccount(token);

            // Chuyển hướng người dùng về trang đăng nhập của FE sau khi kích hoạt thành công
            String redirectUrl = "http://localhost:5173/login?verify_status=success";

            return ResponseEntity
                    .status(HttpStatus.FOUND) // Mã 302 Found cho việc chuyển hướng
                    .header("Location", redirectUrl)
                    .build();

        } catch (IllegalArgumentException e) {
            // Token không hợp lệ/không tồn tại
            String redirectUrl = "http://localhost:5173/verify-result?verify_status=invalid_token";
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            // Lỗi hệ thống
            String redirectUrl = "http://localhost:5173/verify-result?verify_status=error";
            return ResponseEntity
                    .status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        }
    }

    @GetMapping("/activate") // Chọn GET cho đơn giản, vì link email chỉ là GET
    public ResponseEntity<?> activateAccount(@RequestParam String token) {
        try {
            // Gọi Service để xử lý kích hoạt
            authService.verifyAccount(token); // Phương thức này sẽ set isActive=true và xóa token

            // Trả về thành công 200 OK
            return ResponseEntity.ok("Tài khoản đã được kích hoạt thành công.");

        } catch (IllegalArgumentException e) {
            // Token không hợp lệ/không tồn tại
            return ResponseEntity.badRequest().body("Lỗi kích hoạt: Token không hợp lệ.");
        } catch (IllegalStateException e) {
            // Token hết hạn (nếu có logic kiểm tra hết hạn)
            return ResponseEntity.status(HttpStatus.GONE).body("Lỗi kích hoạt: Token đã hết hạn."); // 410 GONE
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống trong quá trình kích hoạt tài khoản.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
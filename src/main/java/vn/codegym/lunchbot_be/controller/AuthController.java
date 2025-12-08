package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.dto.request.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.service.impl.AuthService;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService; // Thêm final và bỏ @Autowired

    @PostMapping("/register/merchant")
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody MerchantRegisterRequest request,
                                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            return ResponseEntity.badRequest().body("Lỗi dữ liệu đầu vào: " + errorMsg);
        }
        try {
            authService.registerMerchant(request);
            return ResponseEntity.ok("Đăng ký Merchant thành công! Vui lòng chờ duyệt.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Đăng ký thất bại do lỗi hệ thống.");
        }
    }
}
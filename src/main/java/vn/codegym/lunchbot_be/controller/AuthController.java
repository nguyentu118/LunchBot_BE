package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.dto.MerchantRegisterRequest;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.service.AuthService;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Autowired
    private AuthService authService;

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
}

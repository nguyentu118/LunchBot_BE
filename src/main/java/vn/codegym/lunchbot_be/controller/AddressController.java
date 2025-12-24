package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.AddressRequest;
import vn.codegym.lunchbot_be.dto.response.AddressResponse;
import vn.codegym.lunchbot_be.service.AddressService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Validated
public class AddressController {

    private final AddressService addressService;

    /**
     * Lấy email user từ Security Context
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        return authentication.getName();
    }

    /**
     * GET /api/addresses
     * Lấy tất cả địa chỉ của user
     */
    @GetMapping
    public ResponseEntity<?> getAllAddresses() {
        try {
            String email = getCurrentUserEmail();

            List<AddressResponse> addresses = addressService.getAllAddressesByUser(email);

            return ResponseEntity.ok(addresses);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách địa chỉ: " + e.getMessage());
        }
    }

    /**
     * GET /api/addresses/{id}
     * Lấy thông tin một địa chỉ cụ thể
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAddressById(@PathVariable("id") Long id) {
        try {
            String email = getCurrentUserEmail();
            AddressResponse address = addressService.getAddressById(email, id);
            return ResponseEntity.ok(address);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin địa chỉ: " + e.getMessage());
        }
    }

    /**
     * GET /api/addresses/default
     * Lấy địa chỉ mặc định
     */
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultAddress() {
        try {
            String email = getCurrentUserEmail();
            AddressResponse address = addressService.getDefaultAddress(email);

            if (address == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Chưa có địa chỉ mặc định");
            }

            return ResponseEntity.ok(address);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy địa chỉ mặc định: " + e.getMessage());
        }
    }

    /**
     * POST /api/addresses
     * Tạo địa chỉ mới
     */
    @PostMapping
    public ResponseEntity<?> createAddress(@Valid @RequestBody AddressRequest request) {
        try {
            String email = getCurrentUserEmail();
            AddressResponse address = addressService.createAddress(email, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(address);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo địa chỉ: " + e.getMessage());
        }
    }

    /**
     * PUT /api/addresses/{id}
     * Cập nhật địa chỉ
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        try {
            String email = getCurrentUserEmail();
            AddressResponse address = addressService.updateAddress(email, id, request);
            return ResponseEntity.ok(address);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật địa chỉ: " + e.getMessage());
        }
    }

    /**
     * DELETE /api/addresses/{id}
     * Xóa địa chỉ
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable Long id) {
        try {
            String email = getCurrentUserEmail();
            addressService.deleteAddress(email, id);
            return ResponseEntity.ok("Đã xóa địa chỉ thành công");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa địa chỉ: " + e.getMessage());
        }
    }

    /**
     * PUT /api/addresses/{id}/default
     * Đặt địa chỉ làm mặc định
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable Long id) {
        try {
            String email = getCurrentUserEmail();
            AddressResponse address = addressService.setDefaultAddress(email, id);
            return ResponseEntity.ok(address);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi đặt địa chỉ mặc định: " + e.getMessage());
        }
    }
}
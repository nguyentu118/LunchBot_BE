// vn.codegym.lunchbot_be.controller.CartController.java (Tạo mới)
package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.AddToCartRequest;
import vn.codegym.lunchbot_be.dto.request.UpdateCartItemRequest;
import vn.codegym.lunchbot_be.dto.response.CartCountResponse;
import vn.codegym.lunchbot_be.dto.response.CartResponse;
import vn.codegym.lunchbot_be.service.CartService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    // Tái sử dụng logic lấy Email từ Security Context tương tự UserController
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        // Lấy tên Principal, là Email/Username
        return authentication.getName();
    }

    // Endpoint: GET /api/cart/count
    @GetMapping("/count")
    public ResponseEntity<CartCountResponse> getCartCount() {
        try {
            // 1. Lấy Email User từ Context
            String email = getCurrentUserEmail();

            // 2. Gọi Service (Service sẽ tự tìm ID và tính tổng)
            Long count = cartService.getCartItemCountByUserEmail(email);

            // 3. Trả về kết quả
            return ResponseEntity.ok(new CartCountResponse(count));

        } catch (SecurityException e) {
            // Xử lý khi chưa đăng nhập (hoặc token không hợp lệ)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new CartCountResponse(0L));
        } catch (Exception e) {
            // Xử lý lỗi chung (User/Cart không tồn tại, DB Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CartCountResponse(0L));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody AddToCartRequest request) {
        try {
            String email = getCurrentUserEmail();
            cartService.addToCart(email, request);
            return ResponseEntity.ok("Đã thêm vào giỏ hàng thành công");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống");
        }
    }

    @GetMapping
    public ResponseEntity<?> getCart() {
        try {
            String email = getCurrentUserEmail();
            CartResponse cartResponse = cartService.getCartByUserEmail(email);
            return ResponseEntity.ok(cartResponse);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống");
        }
    }

    @PutMapping("/update/{dishId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable Long dishId,
            @RequestBody UpdateCartItemRequest request) {
        try {
            String email = getCurrentUserEmail();
            cartService.updateCartItem(email, dishId, request.getQuantity());
            return ResponseEntity.ok("Đã cập nhật số lượng");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống");
        }
    }

    @DeleteMapping("/remove/{dishId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long dishId) {
        try {
            String email = getCurrentUserEmail();
            cartService.removeFromCart(email, dishId);
            return ResponseEntity.ok("Đã xóa món khỏi giỏ hàng");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống");
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearCart() {
        try {
            String email = getCurrentUserEmail();
            cartService.clearCart(email);
            return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống");
        }
    }
}
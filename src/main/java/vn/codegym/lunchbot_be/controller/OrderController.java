package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.response.CheckoutResponse;
import vn.codegym.lunchbot_be.dto.response.OrderResponse;
import vn.codegym.lunchbot_be.service.OrderService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho quản lý đơn hàng và checkout
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    /**
     * Lấy email user từ Security Context
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new SecurityException("Vui lòng đăng nhập để tiếp tục");
        }
        return authentication.getName();
    }

    // ========== CHECKOUT ENDPOINTS ==========

    /**
     * GET /api/checkout
     * Lấy thông tin trang thanh toán
     */
    @GetMapping("/checkout")
    public ResponseEntity<?> getCheckoutInfo(
            @RequestParam(required = false) List<Long> dishIds
    ) {
        try {
            String email = getCurrentUserEmail();
            CheckoutResponse response = orderService.getCheckoutInfo(email, dishIds);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi tải thông tin thanh toán: " + e.getMessage()));
        }
    }

    /**
     * POST /api/checkout/apply-coupon
     * Áp dụng mã giảm giá
     * Body: { "couponCode": "SUMMER2023" }
     */
    @PostMapping("/checkout/apply-coupon")
    public ResponseEntity<?> applyCoupon(
            @RequestParam(required = false) List<Long> dishIds, // ✅ Thêm param
            @RequestBody Map<String, String> request
    ) {
        try {
            String email = getCurrentUserEmail();
            String couponCode = request.get("couponCode");

            if (couponCode == null || couponCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Vui lòng nhập mã giảm giá"));
            }

            CheckoutResponse response = orderService.applyDiscount(email, couponCode, dishIds);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi áp dụng mã giảm giá: " + e.getMessage()));
        }
    }

    // ========== ORDER ENDPOINTS ==========

    /**
     * POST /api/orders
     * Tạo đơn hàng mới
     */
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CheckoutRequest request) {
        try {
            String email = getCurrentUserEmail();
            OrderResponse response = orderService.createOrder(email, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi tạo đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * GET /api/orders
     * Lấy danh sách đơn hàng của user
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        try {
            String email = getCurrentUserEmail();
            List<OrderResponse> orders = orderService.getOrdersByUser(email);
            return ResponseEntity.ok(orders);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * GET /api/orders/{id}
     * Lấy chi tiết một đơn hàng
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        try {
            String email = getCurrentUserEmail();
            OrderResponse order = orderService.getOrderById(email, id);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi lấy thông tin đơn hàng: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/orders/{id}/cancel
     * Hủy đơn hàng
     * Body: { "reason": "Đặt nhầm món" }
     */
    @PutMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String email = getCurrentUserEmail();
            String reason = request.getOrDefault("reason", "Không có lý do");

            OrderResponse order = orderService.cancelOrder(email, id, reason);
            return ResponseEntity.ok(order);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Lỗi khi hủy đơn hàng: " + e.getMessage()));
        }
    }
}
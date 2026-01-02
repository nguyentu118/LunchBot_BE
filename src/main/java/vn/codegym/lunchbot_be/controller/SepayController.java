package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.request.OrderInfoDTO;
import vn.codegym.lunchbot_be.dto.request.SepayWebhookDTO;
import vn.codegym.lunchbot_be.dto.response.OrderResponse;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.service.OrderService;
import vn.codegym.lunchbot_be.service.impl.SepayServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SePay Payment Controller - REAL INTEGRATION
 * Xử lý thanh toán online qua SePay thật
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class SepayController {

    private final SepayServiceImpl sepayService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Value("${sepay.api.token}")
    private String sepayApiToken;

    // Lưu orderInfo trong memory (có thể chuyển sang Redis trong production)
    private static final Map<String, OrderInfoDTO> pendingOrders = new ConcurrentHashMap<>();

    /**
     * Tạo QR thanh toán SePay (REAL)
     */
    @PostMapping("/sepay/create")
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody OrderInfoDTO orderInfo) {
        try {

            // Validate input
            if (orderInfo.getItems() == null || orderInfo.getItems().isEmpty()) {
                log.warn("⚠️ Empty items list");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Danh sách món ăn không được để trống"
                ));
            }

            // Tạo transaction reference
            String txnRef = "SPY" + System.currentTimeMillis();

            // Lưu orderInfo vào memory
            pendingOrders.put(txnRef, orderInfo);
            log.info("💾 Saved order info for txnRef: {}", txnRef);

            // Số tiền (VND)
            long amountInVND = orderInfo.getAmount().longValue();

            // ✅ GỌI REAL SEPAY SERVICE
            Map<String, Object> paymentQR = sepayService.createPaymentQR(amountInVND, txnRef);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentMethod", "sepay");
            response.put("mode", "REAL");
            response.put("txnRef", txnRef);
            response.put("qrCodeUrl", paymentQR.get("qrCodeUrl"));
            response.put("accountNumber", paymentQR.get("accountNumber"));
            response.put("accountName", paymentQR.get("accountName"));
            response.put("bankName", paymentQR.get("bankName"));
            response.put("amount", amountInVND);
            response.put("content", paymentQR.get("content"));

            log.info("✅ [REAL] SePay payment created for txnRef: {}", txnRef);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error creating SePay payment: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Không thể tạo thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán (REAL)
     * Frontend sẽ gọi API này để polling check payment
     */
    @PostMapping("/sepay/check")
    public ResponseEntity<Map<String, Object>> checkPayment(@RequestBody Map<String, Object> requestBody) {
        try {
            String txnRef = (String) requestBody.get("txnRef");
            Long amount = ((Number) requestBody.get("amount")).longValue();

            log.info("🔍 [REAL] Checking payment for txnRef: {}", txnRef);

            // Kiểm tra đơn hàng đã tồn tại chưa
            Optional<Order> existingOrder = orderRepository.findByVnpayTransactionRef(txnRef);

            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                log.info("✅ Order already exists: {}", order.getOrderNumber());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paid", true,
                        "orderId", order.getId(),
                        "orderNumber", order.getOrderNumber(),
                        "message", "Đơn hàng đã được tạo"
                ));
            }

            // ✅ GỌI REAL SEPAY SERVICE ĐỂ CHECK TRANSACTION
            boolean isPaid = sepayService.checkTransaction(txnRef, amount);

            if (isPaid) {
                log.info("💰 [REAL] Payment confirmed for txnRef: {}", txnRef);

                // Lấy orderInfo từ memory
                OrderInfoDTO orderInfo = pendingOrders.get(txnRef);

                if (orderInfo == null) {
                    log.error("❌ Order info not found for txnRef: {}", txnRef);
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "paid", false,
                            "message", "Không tìm thấy thông tin đơn hàng"
                    ));
                }

                // Validate email
                if (orderInfo.getUserEmail() == null || orderInfo.getUserEmail().isEmpty()) {
                    log.error("❌ Invalid order info - missing email");
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "paid", false,
                            "message", "Thông tin đơn hàng không hợp lệ"
                    ));
                }

                // Tạo đơn hàng
                CheckoutRequest checkoutRequest = new CheckoutRequest();
                checkoutRequest.setDishIds(orderInfo.getItems());
                checkoutRequest.setAddressId(orderInfo.getAddressId());
                checkoutRequest.setPaymentMethod(PaymentMethod.CARD);
                checkoutRequest.setNotes(orderInfo.getNotes());
                checkoutRequest.setCouponCode(orderInfo.getCouponCode());

                OrderResponse orderResponse = orderService.createOrder(
                        orderInfo.getUserEmail(),
                        checkoutRequest
                );

                // Cập nhật thông tin thanh toán
                Order order = orderRepository.findById(orderResponse.getId())
                        .orElseThrow(() -> new RuntimeException("Order not found"));

                order.setVnpayTransactionRef(txnRef);
                order.setVnpayAmount(String.valueOf(amount));
                order.setPaymentStatus(PaymentStatus.PAID);
                orderRepository.save(order);

                // Xóa orderInfo khỏi memory
                pendingOrders.remove(txnRef);
                log.info("🗑️ Removed order info from memory for txnRef: {}", txnRef);

                // Lấy thông tin giao dịch chi tiết
                Map<String, Object> transactionDetail = sepayService.getTransactionDetail(txnRef, amount);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("paid", true);
                response.put("orderId", order.getId());
                response.put("orderNumber", order.getOrderNumber());
                response.put("message", "Thanh toán thành công");
                response.put("mode", "REAL");

                if (transactionDetail != null) {
                    response.put("transactionDetail", transactionDetail);
                }

                log.info("✅ [REAL] Order created successfully: {}", order.getOrderNumber());
                return ResponseEntity.ok(response);

            } else {
                log.info("⏳ [REAL] Payment not confirmed yet for txnRef: {}", txnRef);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paid", false,
                        "message", "Chưa nhận được thanh toán"
                ));
            }

        } catch (Exception e) {
            log.error("❌ Error checking payment: ", e);
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "paid", false,
                    "message", "Có lỗi xảy ra: " + e.getMessage()
            ));
        }
    }

    /**
     * Webhook từ SePay (REAL)
     * SePay sẽ gọi API này khi có giao dịch mới
     */
    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSepayWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookDTO webhookData
    ) {
        try {
            log.info("🔔 [WEBHOOK] Received SePay Webhook");
            log.info("🔑 Authorization header: {}", authorization);

            // 1. Bảo mật: Kiểm tra Token
            if (sepayApiToken != null && !sepayApiToken.isEmpty()) {
                if (authorization == null || !authorization.startsWith("Bearer " + sepayApiToken)) {
                    log.error("❌ Invalid SePay API Token!");
                    return ResponseEntity.status(403).body("Unauthorized");
                }
            }


            // 2. Xử lý logic thanh toán
            orderService.processSepayPayment(webhookData);


            // 3. Phản hồi cho SePay biết đã nhận tin (Bắt buộc trả về 200 OK)
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            // Vẫn trả về 200 để SePay không gửi lại (retry) gây spam
            return ResponseEntity.ok(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Test endpoint - Kiểm tra kết nối với SePay API
     */
    @GetMapping("/sepay/test")
    public ResponseEntity<Map<String, Object>> testSepay() {
        try {

            boolean connected = sepayService.testConnection();

            if (connected) {
                // Lấy vài giao dịch gần đây để test
                List<Map<String, Object>> recentTrans = sepayService.getRecentTransactions(5);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "mode", "REAL",
                        "message", "SePay connection successful",
                        "recentTransactions", recentTrans
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Cannot connect to SePay API"
                ));
            }

        } catch (Exception e) {
            log.error("❌ Test failed: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 📊 Debug: Xem tất cả pending orders
     */
    @GetMapping("/sepay/pending-orders")
    public ResponseEntity<Map<String, Object>> getAllPendingOrders() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("mode", "REAL");
        result.put("count", pendingOrders.size());
        result.put("txnRefs", pendingOrders.keySet());
        return ResponseEntity.ok(result);
    }

    /**
     * 🗑️ Clear pending order (Admin only)
     */
    @DeleteMapping("/sepay/pending-orders/{txnRef}")
    public ResponseEntity<Map<String, Object>> clearPendingOrder(@PathVariable String txnRef) {
        OrderInfoDTO removed = pendingOrders.remove(txnRef);

        return ResponseEntity.ok(Map.of(
                "success", removed != null,
                "message", removed != null ? "Cleared" : "Not found"
        ));
    }
}
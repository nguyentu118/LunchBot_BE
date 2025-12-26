package vn.codegym.lunchbot_be.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.CheckoutRequest;
import vn.codegym.lunchbot_be.dto.request.OrderInfoDTO;
import vn.codegym.lunchbot_be.dto.response.OrderResponse;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.service.OrderService;
import vn.codegym.lunchbot_be.service.impl.MockSepayServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SePay Payment Controller - MOCK MODE ONLY
 * Xử lý thanh toán online qua SePay (giả lập)
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class SepayController {

    private final MockSepayServiceImpl mockSepayService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    // ✅ Lưu orderInfo trong memory thay vì session
    private static final Map<String, OrderInfoDTO> pendingOrders = new ConcurrentHashMap<>();

    /**
     * Tạo QR thanh toán SePay (MOCK)
     */
    @PostMapping("/sepay/create")
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody OrderInfoDTO orderInfo,
            HttpServletRequest request
    ) {
        try {
            log.info("📥 Received payment request: {}", orderInfo);

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

            // ✅ Lưu orderInfo vào ConcurrentHashMap (thread-safe)
            pendingOrders.put(txnRef, orderInfo);


            // Số tiền (VND)
            long amountInVND = orderInfo.getAmount().longValue();

            // ✅ CHỈ DÙNG MOCK SERVICE
            Map<String, Object> paymentQR = mockSepayService.createPaymentQR(amountInVND, txnRef);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("paymentMethod", "sepay");
            response.put("mode", "MOCK");
            response.put("txnRef", txnRef);
            response.put("qrCodeUrl", paymentQR.get("qrCodeUrl"));
            response.put("accountNumber", paymentQR.get("accountNumber"));
            response.put("accountName", paymentQR.get("accountName"));
            response.put("bankName", paymentQR.get("bankName"));
            response.put("amount", amountInVND);
            response.put("content", paymentQR.get("content"));

            log.info("✅ SePay payment created for txnRef: {}", txnRef);

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
     * Kiểm tra trạng thái thanh toán (MOCK)
     */
    @PostMapping("/sepay/check")
    public ResponseEntity<Map<String, Object>> checkPayment(
            @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request
    ) {
        try {
            String txnRef = (String) requestBody.get("txnRef");
            Long amount = ((Number) requestBody.get("amount")).longValue();


            // Kiểm tra đơn hàng đã tồn tại chưa
            Optional<Order> existingOrder = orderRepository.findByVnpayTransactionRef(txnRef);

            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paid", true,
                        "orderId", order.getId(),
                        "orderNumber", order.getOrderNumber(),
                        "message", "Đơn hàng đã được tạo"
                ));
            }

            // ✅ CHỈ DÙNG MOCK SERVICE
            boolean isPaid = mockSepayService.checkTransaction(txnRef, amount);


            if (isPaid) {
                // ✅ Lấy orderInfo từ ConcurrentHashMap
                OrderInfoDTO orderInfo = pendingOrders.get(txnRef);

                if (orderInfo == null) {
                    return ResponseEntity.ok(Map.of(
                            "success", false,
                            "paid", false,
                            "message", "Không tìm thấy thông tin đơn hàng"
                    ));
                }


                // Validate email
                if (orderInfo.getUserEmail() == null || orderInfo.getUserEmail().isEmpty()) {
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

                // ✅ Xóa orderInfo khỏi memory
                pendingOrders.remove(txnRef);

                // Cleanup mock transaction
                mockSepayService.clearMockTransaction(txnRef);


                // Lấy thông tin giao dịch chi tiết
                Map<String, Object> transactionDetail =
                        mockSepayService.getTransactionDetail(txnRef, amount);

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("paid", true);
                response.put("orderId", order.getId());
                response.put("orderNumber", order.getOrderNumber());
                response.put("message", "Thanh toán thành công");
                response.put("mode", "MOCK");
                if (transactionDetail != null) {
                    response.put("transactionDetail", transactionDetail);
                }

                return ResponseEntity.ok(response);

            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "paid", false,
                        "message", "Chưa nhận được thanh toán"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "paid", false,
                    "message", "Có lỗi xảy ra: " + e.getMessage()
            ));
        }
    }

    /**
     * 🎮 Manual trigger payment (Dùng để demo nhanh)
     */
    @PostMapping("/sepay/mock/trigger/{txnRef}")
    public ResponseEntity<Map<String, Object>> mockTriggerPayment(@PathVariable String txnRef) {
        boolean triggered = mockSepayService.manualTriggerPayment(txnRef);

        return ResponseEntity.ok(Map.of(
                "success", triggered,
                "message", triggered ? "Payment triggered" : "Transaction not found"
        ));
    }

    /**
     * Test endpoint
     */
    @GetMapping("/sepay/test")
    public ResponseEntity<Map<String, Object>> testSepay() {
        try {
            String txnRef = "TEST" + System.currentTimeMillis();
            long amount = 50000;

            Map<String, Object> qrInfo = mockSepayService.createPaymentQR(amount, txnRef);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "mode", "MOCK",
                    "message", "SePay test successful",
                    "data", qrInfo
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Test failed: " + e.getMessage()
            ));
        }
    }

    /**
     * 📊 Debug: Xem tất cả mock transactions
     */
    @GetMapping("/sepay/mock/transactions")
    public ResponseEntity<Map<String, Object>> getAllMockTransactions() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "transactions", mockSepayService.getAllMockTransactions(),
                "pendingOrders", pendingOrders.size()
        ));
    }

    /**
     * 📊 Debug: Xem tất cả pending orders
     */
    @GetMapping("/sepay/mock/pending-orders")
    public ResponseEntity<Map<String, Object>> getAllPendingOrders() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("count", pendingOrders.size());
        result.put("txnRefs", pendingOrders.keySet());
        return ResponseEntity.ok(result);
    }
}
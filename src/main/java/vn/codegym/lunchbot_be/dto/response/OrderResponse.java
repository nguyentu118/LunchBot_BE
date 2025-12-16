package vn.codegym.lunchbot_be.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    // ========== THÔNG TIN ĐỚN HÀNG ==========
    private Long id;
    private String orderNumber; // Mã đơn hàng (VD: ORD-20231215-001)
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // ========== THÔNG TIN CỬA HÀNG ==========
    private Long merchantId;
    private String merchantName;
    private String merchantAddress;
    private String merchantPhone;

    // ========== ĐỊA CHỈ GIAO HÀNG ==========
    private AddressResponse shippingAddress;

    // ========== DANH SÁCH MÓN ĂN ==========
    private List<OrderItemDTO> items;
    private Integer totalItems;

    // ========== TÍNH TOÁN GIÁ ==========
    private BigDecimal itemsTotal;      // Tổng tiền món ăn
    private BigDecimal discountAmount;  // Số tiền giảm giá
    private BigDecimal serviceFee;      // Phí dịch vụ
    private BigDecimal shippingFee;     // Phí vận chuyển
    private BigDecimal totalAmount;     // Tổng thanh toán

    // ========== MÃ GIẢM GIÁ ==========
    private String couponCode;

    // ========== GHI CHÚ & THỜI GIAN ==========
    private String notes;
    private LocalDateTime orderDate;
    private LocalDateTime expectedDeliveryTime;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
}
package vn.codegym.lunchbot_be.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"order"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ========== SNAPSHOT FIELDS (Lưu thông tin tại thời điểm đặt hàng) ==========

    /**
     * ✅ CHỈ LƯU ID - KHÔNG REFERENCE ENTITY
     * Lý do: Khi Dish bị xóa/sửa, OrderItem không bị ảnh hưởng
     */
    @Column(name = "dish_id", nullable = false)
    private Long dishId;

    /**
     * ✅ SNAPSHOT: Tên món tại thời điểm đặt
     * Ngay cả khi merchant đổi tên món, order cũ vẫn hiển thị tên cũ
     */
    @Column(nullable = false, length = 255)
    private String dishName;

    /**
     * ✅ SNAPSHOT: Ảnh món tại thời điểm đặt
     * Lưu URL của ảnh đầu tiên
     */
    @Column(columnDefinition = "TEXT")
    private String dishImage;

    /**
     * ✅ SNAPSHOT: Thông tin merchant (optional nhưng hữu ích)
     * Để hiển thị lịch sử đơn hàng ngay cả khi merchant thay đổi thông tin
     */
    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    // ========== ORDER ITEM DETAILS ==========

    @Column(nullable = false)
    private Integer quantity;

    /**
     * Giá tại thời điểm đặt (có thể đã giảm giá)
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Tổng tiền = unitPrice * quantity
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    // ========== BUSINESS METHODS ==========\r\n

    /**
     * Tính tổng tiền cho item này
     */
    public BigDecimal calculateTotal() {
        if (this.unitPrice == null || this.quantity == null) {
            return BigDecimal.ZERO;
        }
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }

    private void calculateTotalPrice() {
        this.totalPrice = calculateTotal();
    }

    private void validateBeforePersist() {
        if (this.dishId == null) {
            throw new IllegalStateException("dishId không được null");
        }
        if (this.dishName == null || this.dishName.trim().isEmpty()) {
            throw new IllegalStateException("dishName không được rỗng");
        }
        if (this.quantity == null || this.quantity <= 0) {
            throw new IllegalStateException("quantity phải > 0");
        }
    }

    /**
     * Hàm này chạy trước khi lưu mới (Insert)
     * Gộp cả validation và tính toán vào đây
     */
    @PrePersist
    protected void onPrePersist() {
        validateBeforePersist(); // Validate trước
        calculateTotalPrice();   // Tính tiền sau
    }

    /**
     * Hàm này chạy trước khi cập nhật (Update)
     */
    @PreUpdate
    protected void onPreUpdate() {
        calculateTotalPrice();
    }
}
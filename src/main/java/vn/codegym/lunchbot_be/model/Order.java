package vn.codegym.lunchbot_be.model;

import vn.codegym.lunchbot_be.model.enums.OrderStatus;
import vn.codegym.lunchbot_be.model.enums.PaymentMethod;
import vn.codegym.lunchbot_be.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = "order_number"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "merchant", "orderItems", "transactions"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_partner_id")
    private ShippingPartner shippingPartner;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(precision = 12, scale = 2)
    private BigDecimal itemsTotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal commissionFee = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT", length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime orderDate;

    private LocalDateTime expectedDeliveryTime;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order")
    private List<Transaction> transactions = new ArrayList<>();

    // Business methods
    public void calculateTotal() {
        // Calculate items total
        this.itemsTotal = this.orderItems.stream()
                .map(OrderItem::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate commission
        if (this.merchant != null) {
            this.commissionFee = this.itemsTotal.multiply(this.merchant.getCommissionRate());
        }

        // Calculate final total
        this.totalAmount = this.itemsTotal
                .subtract(this.discountAmount)
                .add(this.serviceFee)
                .add(this.shippingFee)
                .add(this.commissionFee);
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING ||
                this.status == OrderStatus.CONFIRMED;
    }

    public void updateStatus(OrderStatus newStatus) {
        this.status = newStatus;

        switch (newStatus) {
            case COMPLETED:
                this.completedAt = LocalDateTime.now();
                this.paymentStatus = PaymentStatus.PAID;
                break;
            case CANCELLED:
                this.cancelledAt = LocalDateTime.now();
                this.paymentStatus = PaymentStatus.FAILED;
                break;
        }
    }
}

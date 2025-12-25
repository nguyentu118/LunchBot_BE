package vn.codegym.lunchbot_be.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.ColumnDefault;
import vn.codegym.lunchbot_be.model.enums.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"merchant", "orders"})
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    @JsonIgnoreProperties({"coupons", "orders", "dishes", "user", "hibernateLazyInitializer", "handler"})
    private Merchant merchant;

    @Column(nullable = false, unique = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 10)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private Integer usageLimit;

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validTo;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    @ColumnDefault("0")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "coupon")
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    // Business methods
    public boolean isValid() {
        LocalDate today = LocalDate.now();
        return this.isActive &&
                !today.isBefore(this.validFrom) &&
                !today.isAfter(this.validTo) &&
                this.usedCount < this.usageLimit;
    }

    public BigDecimal calculateDiscount(BigDecimal amount) {
        if (!isValid()) {
            throw new IllegalStateException("Coupon is not valid");
        }

        switch (this.discountType) {
            case PERCENTAGE:
                return amount.multiply(this.discountValue.divide(new BigDecimal("100")));
            case FIXED_AMOUNT:
                return this.discountValue.min(amount);
            default:
                return BigDecimal.ZERO;
        }
    }

    public void incrementUsedCount() {
        this.usedCount++;
    }
}

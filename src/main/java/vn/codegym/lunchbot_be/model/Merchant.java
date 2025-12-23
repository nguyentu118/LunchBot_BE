package vn.codegym.lunchbot_be.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "dishes", "orders", "coupons", "transactions",
        "withdrawalRequests", "revenueClaims"})
@EqualsAndHashCode(exclude = {"user", "dishes", "orders", "coupons", "transactions", "withdrawalRequests", "revenueClaims"})
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    private String restaurantName;

    private String address;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(unique = true)
    private String phone;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(precision = 12, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal revenueTotal = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean isPartner = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isLocked = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isApproved = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'PENDING'")
    private MerchantStatus status = MerchantStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    private LocalDateTime registrationDate;

    private LocalDateTime approvalDate;
    private LocalDateTime partnerRequestedAt;
    private LocalDateTime lockedAt;

    @Column(precision = 12, scale = 2)
    @ColumnDefault("0.00")
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(precision = 6, scale = 5)
    @ColumnDefault("0.00001")
    private BigDecimal commissionRate = new BigDecimal("0.00001");

    // Relationships
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Dish> dishes = new ArrayList<>();

    @OneToMany(mappedBy = "merchant")
    @JsonIgnore
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Coupon> coupons = new ArrayList<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WithdrawalRequest> withdrawalRequests = new ArrayList<>();

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RevenueClaim> revenueClaims = new ArrayList<>();

    // Business methods
    public boolean canBecomePartner() {
        return this.revenueTotal.compareTo(new BigDecimal("100000000")) > 0;
    }

    public void updateCommissionRate() {
        if (this.revenueTotal.compareTo(new BigDecimal("200000000")) >= 0) {
            this.commissionRate = new BigDecimal("0.000005"); // 0.0005%
        } else {
            this.commissionRate = new BigDecimal("0.00001"); // 0.001%
        }
    }

    // Thêm method mới
    public void approve(String reason) {
        this.status = MerchantStatus.APPROVED;
        this.isApproved = true;
        this.approvalDate = LocalDateTime.now();
        this.rejectionReason = reason;
        this.user.setIsActive(true); // Kích hoạt user
    }

    public void reject(String reason) {
        this.status = MerchantStatus.REJECTED;
        this.isApproved = false;
        this.rejectionReason = reason;
        this.user.setIsActive(false); // Vô hiệu hóa user
    }

    public void lock(String reason) {
        this.status = MerchantStatus.LOCKED;
        this.isLocked = true;
        this.lockedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        this.user.setIsActive(false); // Chặn đăng nhập
    }

    public void unlock(String reason) {
        this.status = MerchantStatus.APPROVED;
        this.isLocked = false;
        this.rejectionReason = reason;
        this.user.setIsActive(true); // Cho phép đăng nhập
    }
}
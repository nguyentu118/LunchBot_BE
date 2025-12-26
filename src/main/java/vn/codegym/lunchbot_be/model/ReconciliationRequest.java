package vn.codegym.lunchbot_be.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reconciliation_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "year_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"merchant", "reviewedBy"})
public class ReconciliationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "`year_month`", nullable = false, length = 7)
    private String yearMonth; // "2025-12"

    // Thông tin tài chính (snapshot tại thời điểm tạo request)
    @Column(nullable = false)
    private Integer totalOrders;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGrossRevenue;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal platformCommissionRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPlatformFee;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netRevenue;

    // Trạng thái
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus status = ReconciliationStatus.PENDING;

    // Admin review
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    // Ghi chú
    @Column(columnDefinition = "TEXT")
    private String merchantNotes;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    // Timestamps
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Business methods
    public void approve(User admin) {
        if (this.status != ReconciliationStatus.PENDING && this.status != ReconciliationStatus.REPORTED) {
            throw new IllegalStateException("Chỉ có thể duyệt request đang PENDING hoặc REPORTED");
        }
        this.status = ReconciliationStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(User admin, String reason) {
        if (this.status != ReconciliationStatus.PENDING && this.status != ReconciliationStatus.REPORTED) {
            throw new IllegalStateException("Chỉ có thể từ chối request đang PENDING hoặc REPORTED");
        }
        this.status = ReconciliationStatus.REJECTED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public boolean isPending() {
        return this.status == ReconciliationStatus.PENDING;
    }

    public boolean canBeModified() {
        return this.status == ReconciliationStatus.PENDING;
    }
}
package vn.codegym.lunchbot_be.model;

import vn.codegym.lunchbot_be.model.enums.TransactionStatus;
import vn.codegym.lunchbot_be.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"merchant", "order"})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceBefore;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_request_id")
    private ReconciliationRequest reconciliationRequest;

    @CreationTimestamp
    private LocalDateTime transactionDate;

    private LocalDateTime processedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Business method
    public void process() {
        if (this.status == TransactionStatus.PENDING) {
            this.status = TransactionStatus.COMPLETED;
            this.processedAt = LocalDateTime.now();
        }
    }
}

package vn.codegym.lunchbot_be.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "customer_bank_account")
    private String customerBankAccount;

    @Column(name = "customer_bank_name")
    private String customerBankName;

    @Column(name = "customer_account_name")
    private String customerAccountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false)
    private RefundStatus refundStatus = RefundStatus.PENDING;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "transaction_ref")
    private String transactionRef; // Mã giao dịch gốc (SPYxxx)

    @Column(name = "refund_transaction_ref")
    private String refundTransactionRef; // Mã giao dịch hoàn tiền

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private String processedBy; // Email admin xử lý

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
package vn.codegym.lunchbot_be.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.config.SepayConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock SePay Service - Dùng cho DEV/TEST
 * Không cần API key thật, tự động "thanh toán" sau vài giây
 */
@Service
@Slf4j
public class MockSepayServiceImpl {

    private final SepayConfig sepayConfig;

    // Lưu trữ các giao dịch giả lập
    private final Map<String, MockTransaction> mockTransactions = new ConcurrentHashMap<>();

    @Value("${sepay.mock.auto-pay-delay:10}")
    private int autoPayDelaySeconds; // Tự động "thanh toán" sau N giây

    public MockSepayServiceImpl(SepayConfig sepayConfig) {
        this.sepayConfig = sepayConfig;
    }

    /**
     * Tạo nội dung chuyển khoản
     */
    public String generateTransferContent(String txnRef) {
        return "THANHTOAN " + txnRef;
    }

    /**
     * Tạo QR Code thanh toán (MOCK)
     */
    public Map<String, Object> createPaymentQR(long amount, String txnRef) {
        String content = generateTransferContent(txnRef);

        Map<String, Object> response = new HashMap<>();
        response.put("accountNumber", sepayConfig.getAccountNumber());
        response.put("accountName", sepayConfig.getAccountName());
        response.put("bankName", sepayConfig.getBankName());
        response.put("amount", amount);
        response.put("content", content);
        response.put("txnRef", txnRef);

        // Tạo URL QR Code
        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-%d-compact2.jpg?addInfo=%s&accountName=%s",
                sepayConfig.getBankBin(),
                sepayConfig.getAccountNumber(),
                amount,
                encodeURL(content),
                encodeURL(sepayConfig.getAccountName())
        );

        response.put("qrCodeUrl", qrUrl);

        //  Tạo mock transaction
        MockTransaction mockTxn = new MockTransaction(
                txnRef,
                amount,
                content,
                System.currentTimeMillis()
        );
        mockTransactions.put(txnRef, mockTxn);

        log.info("=== 🎭 MOCK TRANSACTION CREATED ===");
        log.info("TxnRef: {}", txnRef);
        log.info("Amount: {} VND", amount);
        log.info("Status: PENDING (will auto-pay in {} seconds)", autoPayDelaySeconds);
        log.info("===================================");

        // 🎭 Tự động "thanh toán" sau N giây
        scheduleAutoPayment(txnRef, autoPayDelaySeconds);

        return response;
    }

    /**
     * Kiểm tra giao dịch (MOCK)
     */
    public boolean checkTransaction(String txnRef, long amount) {
        MockTransaction mockTxn = mockTransactions.get(txnRef);

        if (mockTxn == null) {
            log.warn("⚠️ Mock transaction NOT FOUND: {}", txnRef);
            log.warn("Available transactions: {}", mockTransactions.keySet());
            return false;
        }

        boolean isPaid = mockTxn.isPaid();

        log.info("🔍 Checking transaction: {} | Paid: {} | Amount: {} | Created: {}",
                txnRef, isPaid, mockTxn.getAmount(),
                new Date(mockTxn.getCreatedAt()));

        return isPaid;
    }

    /**
     * Lấy thông tin giao dịch chi tiết (MOCK)
     */
    public Map<String, Object> getTransactionDetail(String txnRef, long amount) {
        MockTransaction mockTxn = mockTransactions.get(txnRef);

        if (mockTxn == null || !mockTxn.isPaid()) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", "MOCK_" + UUID.randomUUID().toString());
        result.put("amount", mockTxn.getAmount());
        result.put("content", mockTxn.getContent());
        result.put("transactionDate", new Date(mockTxn.getCreatedAt()).toString());
        result.put("bankBrandName", "Mock Bank");
        result.put("note", "This is a MOCK transaction for development");

        return result;
    }

    /**
     * 🎭 Tự động "thanh toán" sau N giây
     */
    private void scheduleAutoPayment(String txnRef, int delaySeconds) {
        new Thread(() -> {
            try {
                log.info("⏰ Auto-payment scheduled for {} in {} seconds", txnRef, delaySeconds);

                Thread.sleep(delaySeconds * 1000L);

                MockTransaction mockTxn = mockTransactions.get(txnRef);
                if (mockTxn != null && !mockTxn.isPaid()) {
                    mockTxn.setPaid(true);
                    log.info("✅ 🎭 MOCK AUTO-PAID: {}", txnRef);
                    log.info("Transaction is now PAID and ready to be processed");
                } else if (mockTxn == null) {
                    log.warn("⚠️ Transaction {} not found during auto-payment", txnRef);
                } else {
                    log.info("ℹ️ Transaction {} already paid", txnRef);
                }
            } catch (InterruptedException e) {
                log.error("❌ Error in auto-payment thread for {}", txnRef, e);
            }
        }, "AutoPay-" + txnRef).start();
    }

    /**
     * 🎮 Manual trigger payment (để test)
     */
    public boolean manualTriggerPayment(String txnRef) {
        MockTransaction mockTxn = mockTransactions.get(txnRef);

        if (mockTxn == null) {
            log.warn("⚠️ Cannot trigger payment: transaction {} not found", txnRef);
            return false;
        }

        mockTxn.setPaid(true);
        log.info("✅ MOCK: Manually triggered payment: {}", txnRef);
        return true;
    }

    /**
     * 🧹 Xóa mock transaction (cleanup)
     */
    public void clearMockTransaction(String txnRef) {
        MockTransaction removed = mockTransactions.remove(txnRef);
        if (removed != null) {
            log.info("🧹 Cleared mock transaction: {}", txnRef);
        }
    }

    /**
     * 📊 Lấy tất cả mock transactions (debug)
     */
    public Map<String, MockTransaction> getAllMockTransactions() {
        return new HashMap<>(mockTransactions);
    }

    private String encodeURL(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Mock Transaction Model
     */
    public static class MockTransaction {
        private final String txnRef;
        private final long amount;
        private final String content;
        private final long createdAt;
        private boolean paid;

        public MockTransaction(String txnRef, long amount, String content, long createdAt) {
            this.txnRef = txnRef;
            this.amount = amount;
            this.content = content;
            this.createdAt = createdAt;
            this.paid = false;
        }

        // Getters & Setters
        public String getTxnRef() { return txnRef; }
        public long getAmount() { return amount; }
        public String getContent() { return content; }
        public long getCreatedAt() { return createdAt; }
        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }
}
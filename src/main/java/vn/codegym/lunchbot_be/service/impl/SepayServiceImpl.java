package vn.codegym.lunchbot_be.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.codegym.lunchbot_be.config.SepayConfig;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Real SePay Service - Tích hợp thật với SePay API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SepayServiceImpl {

    private final SepayConfig sepayConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sepay.api.token}")
    private String apiToken;

    @Value("${sepay.api.base-url:https://my.sepay.vn/userapi}")
    private String baseUrl;

    @Value("${sepay.account.id}")
    private String accountId;

    // ✅ SePay QR API chính thức
    // Format: https://qr.sepay.vn/img?acc={ACCOUNT_ID}&bank={BANK}&amount={AMOUNT}&des={DESCRIPTION}&template=compact
    private static final String SEPAY_QR_TEMPLATE =
            "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=compact";

    /**
     * Tạo QR Code thanh toán SePay
     * @param amountInVND - Số tiền (VND)
     * @param txnRef - Mã giao dịch
     * @return Map chứa thông tin QR và tài khoản
     */
    public Map<String, Object> createPaymentQR(long amountInVND, String txnRef) {
        try {
            log.info("🔵 Creating REAL SePay payment for txnRef: {}, amount: {}", txnRef, amountInVND);

            String content = "THANHTOAN " + txnRef;
            String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);

            // ✅ SỬ DỤNG SEPAY QR API (KHÔNG PHẢI VietQR)
            String qrCodeUrl = String.format(
                    SEPAY_QR_TEMPLATE,
                    accountId,                    // Account ID: 962475P4HB
                    sepayConfig.getBankName(),    // Bank: BIDV
                    amountInVND,                  // Số tiền
                    encodedContent                // Nội dung: THANHTOAN SPYxxx
            );

            log.info("🎯 Generated SePay QR URL: {}", qrCodeUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("qrCodeUrl", qrCodeUrl);
            response.put("accountNumber", sepayConfig.getAccountNumber());
            response.put("accountName", sepayConfig.getAccountName());
            response.put("bankName", sepayConfig.getBankName());
            response.put("bankBin", sepayConfig.getBankBin());
            response.put("amount", amountInVND);
            response.put("content", content);
            response.put("txnRef", txnRef);

            log.info("✅ Created REAL payment QR successfully");
            return response;

        } catch (Exception e) {
            log.error("❌ Error creating payment QR: ", e);
            throw new RuntimeException("Không thể tạo mã QR thanh toán: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra giao dịch có được thanh toán chưa
     * Gọi API SePay để check transactions
     * @param txnRef - Mã giao dịch
     * @param expectedAmount - Số tiền cần thanh toán
     * @return true nếu đã thanh toán
     */
    public boolean checkTransaction(String txnRef, long expectedAmount) {
        try {
            log.info("🔍 Checking REAL transaction: {}", txnRef);
            log.info("   Expected amount: {}", expectedAmount);

            // Gọi API SePay để lấy danh sách giao dịch
            String url = baseUrl + "/transactions/list";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode transactions = root.get("transactions");

                if (transactions != null && transactions.isArray()) {
                    log.info("📋 Found {} transactions from SePay", transactions.size());

                    // Log 3 transactions đầu tiên để debug
                    for (int i = 0; i < Math.min(3, transactions.size()); i++) {
                        JsonNode trans = transactions.get(i);
                        log.info("   Transaction #{}: content='{}', amount={}",
                                i + 1,
                                trans.has("transaction_content") ? trans.get("transaction_content").asText() : "N/A",
                                trans.has("amount_in") ? trans.get("amount_in").asLong() : 0
                        );
                    }

                    for (JsonNode transaction : transactions) {
                        if (!transaction.has("transaction_content") || !transaction.has("amount_in")) {
                            continue;
                        }

                        String transContent = transaction.get("transaction_content").asText();
                        long transAmount = transaction.get("amount_in").asLong();

                        // ✅ FLEXIBLE MATCHING - Chỉ cần chứa txnRef (không phân biệt hoa thường)
                        String normalizedContent = transContent.toUpperCase().replaceAll("\\s+", "");
                        String normalizedTxnRef = txnRef.toUpperCase();

                        log.debug("🔍 Comparing: [{}] contains [{}]? {}",
                                normalizedContent, normalizedTxnRef, normalizedContent.contains(normalizedTxnRef));

                        // Kiểm tra nội dung chuyển khoản và số tiền
                        if (normalizedContent.contains(normalizedTxnRef) && transAmount >= expectedAmount) {
                            log.info("✅ ✅ ✅ FOUND MATCHING TRANSACTION!");
                            log.info("   Content: {}", transContent);
                            log.info("   Amount: {} >= {}", transAmount, expectedAmount);
                            return true;
                        }
                    }

                    log.warn("⚠️ No matching transaction found for txnRef: {}", txnRef);
                    log.warn("   Expected amount: {}", expectedAmount);
                } else {
                    log.warn("⚠️ No transactions array in response");
                }
            } else {
                log.error("❌ SePay API error: {}", response.getStatusCode());
            }

            log.info("⏳ Transaction not found yet for {}", txnRef);
            return false;

        } catch (Exception e) {
            log.error("❌ Error checking transaction: ", e);
            return false;
        }
    }

    /**
     * Lấy chi tiết giao dịch từ SePay
     * @param txnRef - Mã giao dịch
     * @param expectedAmount - Số tiền
     * @return Map chứa thông tin giao dịch
     */
    public Map<String, Object> getTransactionDetail(String txnRef, long expectedAmount) {
        try {
            log.info("📊 Getting transaction detail for: {}", txnRef);

            String url = baseUrl + "/transactions/list";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode transactions = root.get("transactions");

                if (transactions != null && transactions.isArray()) {
                    for (JsonNode transaction : transactions) {
                        if (!transaction.has("transaction_content") || !transaction.has("amount_in")) {
                            continue;
                        }

                        String transContent = transaction.get("transaction_content").asText();
                        long transAmount = transaction.get("amount_in").asLong();

                        // ✅ FLEXIBLE MATCHING
                        String normalizedContent = transContent.toUpperCase().replaceAll("\\s+", "");
                        String normalizedTxnRef = txnRef.toUpperCase();

                        if (normalizedContent.contains(normalizedTxnRef) && transAmount >= expectedAmount) {
                            Map<String, Object> detail = new HashMap<>();
                            detail.put("id", transaction.has("id") ? transaction.get("id").asText() : "N/A");
                            detail.put("transaction_date", transaction.has("transaction_date") ?
                                    transaction.get("transaction_date").asText() : "N/A");
                            detail.put("account_number", transaction.has("account_number") ?
                                    transaction.get("account_number").asText() : "N/A");
                            detail.put("amount_in", transAmount);
                            detail.put("transaction_content", transContent);
                            detail.put("reference_code", transaction.has("reference_number") ?
                                    transaction.get("reference_number").asText() : "N/A");
                            detail.put("bank_brand_name", transaction.has("bank_brand_name") ?
                                    transaction.get("bank_brand_name").asText() : "N/A");

                            log.info("✅ Found transaction detail");
                            return detail;
                        }
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("❌ Error getting transaction detail: ", e);
            return null;
        }
    }

    /**
     * Test connection với SePay API
     * @return true nếu kết nối thành công
     */
    public boolean testConnection() {
        try {
            log.info("🧪 Testing SePay API connection...");

            String url = baseUrl + "/transactions/list";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            boolean success = response.getStatusCode() == HttpStatus.OK;

            if (success) {
                log.info("✅ SePay API connection successful");

                // Log sample transaction
                try {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode transactions = root.get("transactions");
                    if (transactions != null && transactions.isArray() && transactions.size() > 0) {
                        JsonNode firstTrans = transactions.get(0);
                        log.info("📋 Sample transaction: {}", firstTrans.toString());
                    }
                } catch (Exception e) {
                    log.warn("Could not parse sample transaction");
                }
            } else {
                log.error("❌ SePay API connection failed with status: {}", response.getStatusCode());
            }

            return success;

        } catch (Exception e) {
            log.error("❌ SePay API connection error: ", e);
            return false;
        }
    }

    /**
     * Lấy danh sách giao dịch gần đây
     * @param limit - Số lượng giao dịch cần lấy
     * @return List các giao dịch
     */
    public List<Map<String, Object>> getRecentTransactions(int limit) {
        try {
            log.info("📋 Getting recent {} transactions", limit);

            String url = baseUrl + "/transactions/list";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode transactions = root.get("transactions");

                List<Map<String, Object>> result = new ArrayList<>();

                if (transactions != null && transactions.isArray()) {
                    int count = 0;
                    for (JsonNode transaction : transactions) {
                        if (count >= limit) break;

                        Map<String, Object> trans = new HashMap<>();
                        trans.put("id", transaction.has("id") ? transaction.get("id").asText() : "N/A");
                        trans.put("date", transaction.has("transaction_date") ?
                                transaction.get("transaction_date").asText() : "N/A");
                        trans.put("amount", transaction.has("amount_in") ?
                                transaction.get("amount_in").asLong() : 0);
                        trans.put("content", transaction.has("transaction_content") ?
                                transaction.get("transaction_content").asText() : "N/A");
                        trans.put("bank", transaction.has("bank_brand_name") ?
                                transaction.get("bank_brand_name").asText() : "N/A");

                        result.add(trans);
                        count++;
                    }
                }

                log.info("✅ Retrieved {} recent transactions", result.size());
                return result;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Error getting recent transactions: ", e);
            return Collections.emptyList();
        }
    }
}
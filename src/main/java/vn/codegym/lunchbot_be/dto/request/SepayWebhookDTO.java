package vn.codegym.lunchbot_be.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SepayWebhookDTO {
    private Long id; // ID giao dịch bên SePay

    @JsonProperty("gateway")
    private String gateway; // Ngân hàng (MB, VCB...)

    @JsonProperty("transactionDate")
    private String transactionDate;

    @JsonProperty("accountNumber")
    private String accountNumber;

    @JsonProperty("subAccount")
    private String subAccount;

    @JsonProperty("transferAmount")
    private BigDecimal transferAmount; // Số tiền khách chuyển

    @JsonProperty("transferContent")
    private String transferContent; // Nội dung ck (Quan trọng: Chứa Mã đơn hàng)

    @JsonProperty("referenceCode")
    private String referenceCode;

    @JsonProperty("description")
    private String description;
}
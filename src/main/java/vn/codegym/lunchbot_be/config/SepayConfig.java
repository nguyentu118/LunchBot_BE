package vn.codegym.lunchbot_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SepayConfig {

    @Value("${sepay.account.number}")
    private String accountNumber;

    @Value("${sepay.account.name}")
    private String accountName;

    @Value("${sepay.bank.bin:970422}")
    private String bankBin;

    @Value("${sepay.bank.name:MB}")
    private String bankName;

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public String getAccountName() { return accountName; }
    public String getBankBin() { return bankBin; }
    public String getBankName() { return bankName; }
}
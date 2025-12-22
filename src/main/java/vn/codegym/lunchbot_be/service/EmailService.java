package vn.codegym.lunchbot_be.service;

public interface EmailService {

    void sendMerchantApprovalEmail(String merchantEmail, String merchantName,
                                   String restaurantName, String reason);

    void sendMerchantRejectionEmail(String merchantEmail, String merchantName,
                                    String restaurantName, String reason);

    void sendMerchantLockedEmail(String merchantEmail, String merchantName,
                                 String restaurantName, String reason);

    void sendMerchantUnlockedEmail(String merchantEmail, String merchantName,
                                   String restaurantName, String reason);

    void sendWelcomeEmail(String userEmail, String userName);

    void sendPasswordResetEmail(String userEmail, String userName, String resetToken);

    void sendOrderConfirmationEmail(String userEmail, String userName, String orderDetails);

    void sendOrderStatusUpdateEmail(String userEmail, String userName, String orderStatus, String orderDetails);

    void sendRegistrationSuccessEmail(String to, String fullName, String restaurantName, String loginUrl, boolean isMerchant);

    void sendVerificationEmail(String to, String fullName, String token);

    void sendShippingPartnerLockedEmail(String partnerEmail, String partnerName, String reason);

    void sendShippingPartnerUnlockedEmail(String partnerEmail, String partnerName, String reason);
}

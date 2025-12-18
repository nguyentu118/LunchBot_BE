package vn.codegym.lunchbot_be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.repository.CouponRepository;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private final CouponRepository couponRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Production: mỗi đêm 00:00
    //@Scheduled(fixedRate = 120000) // Test: mỗi 2 phút
    @Transactional
    public void deactivateExpiredCoupons() {
        log.info("🕐 Bắt đầu kiểm tra coupon hết hạn - {}", LocalDate.now());

        try {
            LocalDate today = LocalDate.now();

            List<Coupon> expiredCoupons = couponRepository
                    .findByIsActiveTrueAndValidToBefore(today);

            if (expiredCoupons.isEmpty()) {
                log.info("✅ Không có coupon nào hết hạn");
                return;
            }

            // Vô hiệu hóa các coupon hết hạn
            expiredCoupons.forEach(coupon -> {
                coupon.setIsActive(false);
                log.info("❌ Vô hiệu hóa coupon: {} - Mã: {} - Hết hạn: {}",
                        coupon.getId(),
                        coupon.getCode(),
                        coupon.getValidTo()); // ✅ Dùng getValidTo()
            });
            couponRepository.saveAll(expiredCoupons);

            log.info("✅ Đã vô hiệu hóa {} coupon hết hạn", expiredCoupons.size());

        } catch (Exception e) {
            log.error("❌ Lỗi khi kiểm tra coupon hết hạn: {}", e.getMessage(), e);
        }
    }
}
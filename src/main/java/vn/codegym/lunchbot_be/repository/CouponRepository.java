package vn.codegym.lunchbot_be.repository;

import vn.codegym.lunchbot_be.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    List<Coupon> findByMerchantId(Long merchantId);

    boolean existsByCode(String code);

    List<Coupon> findByIsActiveTrueAndValidToBefore(LocalDate date);

    Optional<Coupon> findByCodeAndMerchantId(String code, Long merchantId);

    @Query("SELECT c FROM Coupon c WHERE c.merchant.id = :merchantId AND c.isActive = true AND c.validTo >= CURRENT_DATE AND c.usedCount < c.usageLimit")
    List<Coupon> findActiveCouponsByMerchant(Long merchantId);

    @Query("SELECT c FROM Coupon c WHERE c.merchant.id = :merchantId " +
            "AND c.isActive = true AND c.validFrom <= :today AND c.validTo >= :today")
    List<Coupon> findActiveCouponsByMerchant(@Param("merchantId") Long merchantId,
                                             @Param("today") LocalDate today);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.merchant.id = :merchantId " +
            "AND c.usedCount >= c.usageLimit")
    Long countExpiredCoupons(@Param("merchantId") Long merchantId);
}

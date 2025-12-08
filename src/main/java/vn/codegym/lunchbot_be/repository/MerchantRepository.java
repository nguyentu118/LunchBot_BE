package vn.codegym.lunchbot_be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);
    Optional<Merchant> findByPhone(String phone);

    @Query("SELECT m FROM Merchant m WHERE m.restaurantName LIKE %:keyword%")
    Page<Merchant> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT m FROM Merchant m WHERE m.isPartner = :isPartner")
    List<Merchant> findByPartnerStatus(@Param("isPartner") boolean isPartner);
    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    @Query("SELECT m FROM Merchant m WHERE " +
            "LOWER(m.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Merchant> searchMerchants(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.status = 'PENDING'")
    Long countPendingMerchants();

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.isLocked = true")
    Long countLockedMerchants();

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.isApproved = true")
    Long countApprovedMerchants();

    @Query("SELECT m FROM Merchant m WHERE m.status = :status " +
            "ORDER BY m.registrationDate DESC")
    List<Merchant> findByStatusOrderByRegistrationDateDesc(@Param("status") MerchantStatus status);

    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.registrationDate >= :startDate")
    Long countNewMerchantsThisMonth(@Param("startDate") java.time.LocalDateTime startDate);
}

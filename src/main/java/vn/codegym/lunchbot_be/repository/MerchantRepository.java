package vn.codegym.lunchbot_be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByUserId(Long userId);

    @Query("SELECT m FROM Merchant m JOIN m.user u WHERE u.email = :username")
    Optional<Merchant> findByUsername(@Param("username") String username);

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

    @Query("SELECT new vn.codegym.lunchbot_be.dto.response.PopularMerchantDto(" +
            "m.id, " +
            "m.restaurantName, " +
            "m.address, " +
            "(SELECT MIN(di.imageUrl) FROM DishImage di WHERE di.dish.merchant.id = m.id), " +
            "MIN(d.price), " +
            "MAX(d.price), " +
            "SUM(d.orderCount)) " +
            "FROM Merchant m " +
            "LEFT JOIN m.dishes d " +
            "WHERE m.status = 'APPROVED' " +
            "AND m.isLocked = false " +
            "AND d.isActive = true " +
            "GROUP BY m.id, m.restaurantName, m.address " +
            "HAVING SUM(d.orderCount) > 0 " +
            "ORDER BY SUM(d.orderCount) DESC")
    List<PopularMerchantDto> findPopularMerchants(Pageable pageable);

    /**
     * Alternative query - Đơn giản hơn, chỉ lấy merchant có dishes
     */
    @Query("SELECT DISTINCT m FROM Merchant m " +
            "LEFT JOIN FETCH m.dishes d " +
            "WHERE m.status = 'APPROVED' " +
            "AND m.isLocked = false " +
            "AND d.isActive = true " +
            "ORDER BY m.revenueTotal DESC")
    List<Merchant> findApprovedMerchantsWithActiveDishes(Pageable pageable);
}

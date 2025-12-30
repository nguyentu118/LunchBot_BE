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
import vn.codegym.lunchbot_be.model.enums.PartnerStatus;

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
            "'' as imageUrl, " + // ✅ Để rỗng, ta sẽ lấy ảnh sau (tránh join sai bảng)
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
            "LEFT JOIN FETCH d.images " +  // ✅ FETCH luôn images
            "WHERE m.status = 'APPROVED' " +
            "AND m.isLocked = false " +
            "AND d.isActive = true " +
            "ORDER BY m.revenueTotal DESC")
    List<Merchant> findApprovedMerchantsWithActiveDishes(Pageable pageable);


    /**
     * Lấy tất cả category names của một merchant (qua dishes)
     * @param merchantId ID của merchant
     * @return Danh sách tên categories (distinct)
     */
    @Query("SELECT DISTINCT c.name " +
            "FROM Merchant m " +
            "JOIN m.dishes d " +
            "JOIN d.categories c " +
            "WHERE m.id = :merchantId " +
            "AND d.isActive = true " +
            "ORDER BY c.name ASC")
    List<String> findCategoryNamesByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * Lấy ảnh đầu tiên từ các dishes của merchant
     * @param merchantId ID của merchant
     * @return URL ảnh đầu tiên
     */
    @Query("SELECT di.imageUrl " +
            "FROM DishImage di " +
            "JOIN di.dish d " +
            "WHERE d.merchant.id = :merchantId " +
            "AND d.isActive = true " +
            "AND di.imageUrl IS NOT NULL " +  // ✅ Chỉ lấy ảnh không null
            "ORDER BY di.displayOrder ASC, di.id ASC")
    List<String> findFirstImageByMerchantId(@Param("merchantId") Long merchantId);

    @Query("SELECT COUNT(di) " +
            "FROM DishImage di " +
            "WHERE di.dish.merchant.id = :merchantId " +
            "AND di.dish.isActive = true")
    Long countImagesByMerchantId(@Param("merchantId") Long merchantId);

    @Query(value = "SELECT d.images_urls " +
            "FROM dishes d " +
            "WHERE d.merchant_id = :merchantId " +
            "AND d.is_active = true " +
            "AND d.images_urls IS NOT NULL " +
            "AND d.images_urls != '[]' " +
            "LIMIT 1", nativeQuery = true)
    List<String> findRawImageJsonByMerchantId(@Param("merchantId") Long merchantId);

    List<Merchant> findByPartnerStatus(PartnerStatus partnerStatus);

    Page<Merchant> findAllByOrderByIdDesc(Pageable pageable);

    // Tìm kiếm theo keyword
    @Query("SELECT m FROM Merchant m WHERE " +
            "LOWER(m.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY m.id DESC")
    Page<Merchant> searchMerchantsWithPagination(@Param("keyword") String keyword, Pageable pageable);
}

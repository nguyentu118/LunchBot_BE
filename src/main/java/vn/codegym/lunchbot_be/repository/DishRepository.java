package vn.codegym.lunchbot_be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.model.Category;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    @EntityGraph(attributePaths = {"images"})
    Optional<Dish> findById(Long id);

    List<Dish> findByMerchantId(Long merchantId);

    @Query(value = "SELECT d FROM Dish d WHERE d.isRecommended = true AND d.isActive = true ORDER BY d.orderCount DESC LIMIT 8")
    List<Dish> findTop8SuggestedDishes();

    // --- [TASK 41] Method MỚI: Lấy top 8 món ăn giảm giá nhiều nhất ---
    @Query(value = "SELECT d FROM Dish d " +
            "WHERE d.discountPrice IS NOT NULL " +
            "AND d.isActive = true " +
            "AND d.discountPrice < d.price " +
            "ORDER BY (d.price - d.discountPrice) / d.price DESC " + // Sắp xếp theo (Giá gốc - Giá giảm) / Giá gốc DESC
            "LIMIT 8")
    List<Dish> findTop8MostDiscountedDishes();

    Page<Dish> findByMerchantId(Long merchantId, Pageable pageable);

    Page<Dish> findByCategoriesContainingAndIsActiveTrueAndIdNot(
            Category category,
            Long dishId,
            Pageable pageable
    );


    Page<Dish> findByIsActiveTrueOrderByViewCountDesc(Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "LEFT JOIN FETCH d.merchant m " +
            "LEFT JOIN FETCH d.images i " +
            "WHERE d.id = :dishId")
    Optional<Dish> findByIdWithDetails(@Param("dishId") Long dishId);

    @Query("SELECT d FROM Dish d WHERE d.isRecommended = true AND d.isActive = true")
    List<Dish> findRecommendedDishes();

    @Query("SELECT d FROM Dish d WHERE d.name LIKE %:keyword% AND d.isActive = true")
    Page<Dish> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d WHERE d.price BETWEEN :minPrice AND :maxPrice AND d.isActive = true")
    List<Dish> findByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT d FROM Dish d WHERE d.merchant.id = :merchantId AND d.isActive = true")
    Page<Dish> findActiveDishesByMerchant(@Param("merchantId") Long merchantId, Pageable pageable);
}

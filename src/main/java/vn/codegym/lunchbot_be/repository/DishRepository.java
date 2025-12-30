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

    @Query(value = "SELECT d FROM Dish d " +
            "WHERE d.discountPrice IS NOT NULL " +
            "AND d.isActive = true " +
            "AND d.discountPrice < d.price " +
            "ORDER BY (d.price - d.discountPrice) / d.price DESC " +
            "LIMIT 8")
    List<Dish> findTop8MostDiscountedDishes();

    Page<Dish> findByMerchantId(Long merchantId, Pageable pageable);

    Page<Dish> findByCategoriesContainingAndIsActiveTrueAndIdNot(
            Category category,
            Long dishId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT d FROM Dish d " +
            "LEFT JOIN d.categories c " +
            "WHERE d.merchant.id = :merchantId " +
            "AND d.isActive = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:minPrice IS NULL OR d.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR d.price <= :maxPrice)")
    Page<Dish> searchDishes(
            @Param("merchantId") Long merchantId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    @Query("SELECT d FROM Dish d " +
            "LEFT JOIN d.categories c " +
            "WHERE d.isActive = true " +
            "AND (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName)) " +
            "AND (:minPrice IS NULL OR d.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR d.price <= :maxPrice) " +
            "AND (:isRecommended IS NULL OR d.isRecommended = :isRecommended)")
    Page<Dish> searchDishesWithFilters(
            @Param("name") String name,
            @Param("categoryName") String categoryName,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("isRecommended") Boolean isRecommended,
            Pageable pageable
    );

    @Query("SELECT d FROM Dish d JOIN d.categories c " +
            "WHERE (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
            "AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))")
    List<Dish> searchDishes(@Param("name") String name,
                            @Param("categoryName") String categoryName);

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

    List<Dish> findByMerchantIdAndIsActiveTrue(Long merchantId);

    // ✅ CẢI THIỆN: Sort discount theo nhiều tiêu chí
    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.discountPrice IS NOT NULL " +
            "AND d.discountPrice < d.price " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY (d.price - d.discountPrice) / d.price DESC")
    Page<Dish> findDiscountedDishesOrderByDiscountDesc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.discountPrice IS NOT NULL " +
            "AND d.discountPrice < d.price " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY (d.price - d.discountPrice) / d.price ASC")
    Page<Dish> findDiscountedDishesOrderByDiscountAsc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.discountPrice IS NOT NULL " +
            "AND d.discountPrice < d.price " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY d.discountPrice ASC")
    Page<Dish> findDiscountedDishesOrderByPriceAsc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.discountPrice IS NOT NULL " +
            "AND d.discountPrice < d.price " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY d.discountPrice DESC")
    Page<Dish> findDiscountedDishesOrderByPriceDesc(@Param("keyword") String keyword, Pageable pageable);

    // ✅ CẢI THIỆN: Sort suggested theo nhiều tiêu chí
    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.isRecommended = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY d.orderCount DESC, d.viewCount DESC")
    Page<Dish> findRecommendedDishesOrderByOrderCount(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.isRecommended = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY d.viewCount DESC, d.orderCount DESC")
    Page<Dish> findRecommendedDishesOrderByViewCount(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.isRecommended = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY COALESCE(d.discountPrice, d.price) ASC")
    Page<Dish> findRecommendedDishesOrderByPriceAsc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT d FROM Dish d " +
            "WHERE d.isActive = true " +
            "AND d.isRecommended = true " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(d.merchant.restaurantName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY COALESCE(d.discountPrice, d.price) DESC")
    Page<Dish> findRecommendedDishesOrderByPriceDesc(@Param("keyword") String keyword, Pageable pageable);
}
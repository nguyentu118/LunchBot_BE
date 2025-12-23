package vn.codegym.lunchbot_be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.tags.form.SelectTag;
import vn.codegym.lunchbot_be.model.Order;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findByMerchantId(Long merchantId);

    Page<Order> findByMerchantId(Long merchantId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    List<Order> findByMerchantIdAndStatus(Long merchantId, OrderStatus status);

    Long countByMerchantId(Long merchantId);

    Long countByMerchantIdAndStatus(Long merchantId, Enum status);

    @Query("SELECT o FROM Order o WHERE o.orderNumber LIKE %:keyword% " +
            "OR o.user.email LIKE %:keyword% OR o.user.phone LIKE %:keyword%")
    List<Order> searchOrders(@Param("keyword") String keyword);

    @Query("SELECT COUNT(o), o.status FROM Order o WHERE o.merchant.id = :merchantId GROUP BY o.status")
    List<Object[]> countOrdersByStatus(@Param("merchantId") Long merchantId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND MONTH(o.orderDate) = :month AND YEAR(o.orderDate) = :year")
    BigDecimal getMonthlyRevenue(@Param("merchantId") Long merchantId,
                                 @Param("month") int month,
                                 @Param("year") int year);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    Long getTodayOrderCount(@Param("merchantId") Long merchantId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND DATE(o.orderDate) = CURRENT_DATE")
    BigDecimal getTodayRevenue(@Param("merchantId") Long merchantId);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE oi.dishId = :dishId " +
            "AND o.status NOT IN ('COMPLETED', 'CANCELLED')")
    Long countPendingOrdersByDishId(@Param("dishId") Long dishId);

    // 1. Tính tổng doanh thu theo khoảng ngày (dùng cho cả Tuần/Tháng/Quý)
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND o.status = 'COMPLETED' " + // Chỉ tính đơn đã hoàn thành
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByDateRange(@Param("merchantId") Long merchantId,
                                     @Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // 2. Lấy danh sách đơn hàng theo khoảng ngày (có phân trang cho phần hiển thị bên dưới)
    @Query("SELECT o FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND o.status = 'COMPLETED' " +
            "AND o.orderDate BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersByDateRange(@Param("merchantId") Long merchantId,
                                      @Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.orderItems oi " +
            "WHERE o.merchant.id = :merchantId " +
            "AND oi.dishId = :dishId")
    Page<Order> findOrdersByDishId(
            @Param("merchantId") Long merchantId,
            @Param("dishId") Long dishId,
            Pageable pageable
    );

    // 1. Lấy danh sách khách hàng DUY NHẤT đã từng đặt hàng tại Merchant này
    @Query("SELECT DISTINCT o.user FROM Order o WHERE o.merchant.id = :merchantId")
    List<User> findDistinctCustomersByMerchantId(@Param("merchantId") Long merchantId);

    // 2. Lấy danh sách đơn hàng của một khách hàng cụ thể tại một Merchant cụ thể
    List<Order> findByUserIdAndMerchantId(Long userId, Long merchantId);

    @Query("SELECT o FROM Order o WHERE o.coupon.id = :couponId AND o.merchant.id = :merchantId ORDER BY o.orderDate DESC")
    List<Order> findByCouponIdAndMerchantId(@Param("couponId") Long couponId, @Param("merchantId") Long merchantId);
}


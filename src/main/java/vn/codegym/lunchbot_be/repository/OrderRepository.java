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

    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM Order o WHERE o.shippingAddress.id = :addressId AND o.status IN :statuses")
    boolean existsByShippingAddressIdAndStatusIn(
            @Param("addressId") Long addressId,
            @Param("statuses") List<OrderStatus> statuses);

    @Query(value =
            "SELECT * FROM orders o WHERE o.merchant_id = :merchantId " +
                    "ORDER BY " +
                    "  CASE " +
                    "    WHEN o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING') THEN 1 " +  // Nhóm cần xử lý ngay
                    "    WHEN o.status IN ('READY', 'DELIVERING') THEN 2 " +                  // Nhóm đang hoàn thiện
                    "    WHEN o.status IN ('COMPLETED', 'CANCELLED') THEN 3 " +               // Nhóm đã kết thúc
                    "    ELSE 99 " +
                    "  END, " +
                    // Ưu tiên 2: Theo trạng thái
                    "  CASE o.status " +
                    "    WHEN 'PENDING' THEN 1 " +           // Đơn chờ xác nhận
                    "    WHEN 'CONFIRMED' THEN 2 " +         // Đơn đã xác nhận
                    "    WHEN 'PROCESSING' THEN 3 " +        // Đơn đang chế biến
                    "    WHEN 'READY' THEN 4 " +             // Đơn đã xong món
                    "    WHEN 'DELIVERING' THEN 5 " +        // Đơn đang giao
                    "    WHEN 'COMPLETED' THEN 6 " +         // Đơn hoàn thành
                    "    WHEN 'CANCELLED' THEN 7 " +         // Đơn đã hủy
                    "    ELSE 99 " +
                    "  END, " +
                    // Ưu tiên 1: Đơn hôm nay lên đầu
                    "  CASE WHEN DATE(o.order_date) = CURRENT_DATE THEN 0 ELSE 1 END, " +
                    // Ưu tiên 3: Trong cùng nhóm, đơn mới nhất lên trước
                    "  o.order_date DESC",
            nativeQuery = true)
    List<Order> findByMerchantIdWithPriority(@Param("merchantId") Long merchantId);

    @Query(value =
            "SELECT * FROM orders o WHERE o.merchant_id = :merchantId AND o.status = :status " +
                    "ORDER BY o.order_date DESC",
            nativeQuery = true)
    List<Order> findByMerchantIdAndStatusOrderByDateDesc(
            @Param("merchantId") Long merchantId,
            @Param("status") String status
    );

    @Query("SELECT o FROM Order o WHERE o.merchant.id = :merchantId " +
            "AND o.status = 'COMPLETED' " +
            "AND o.completedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY o.completedAt DESC")
    List<Order> findCompletedOrdersByDateRange(
            @Param("merchantId") Long merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    // ... các imports

    /**
     * Tính tổng doanh thu thực nhận (ItemsTotal - Discount) của Merchant trong khoảng thời gian
     * Chỉ tính đơn hàng COMPLETED
     */
    @Query("SELECT SUM(o.itemsTotal - COALESCE(o.discountAmount, 0)) " +
            "FROM Order o " +
            "WHERE o.merchant.id = :merchantId " +
            "AND o.status = 'COMPLETED' " +
            "AND o.completedAt BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByMerchantAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}


package vn.codegym.lunchbot_be.repository;

import vn.codegym.lunchbot_be.model.Notification;
import vn.codegym.lunchbot_be.model.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ===== QUERY THEO USER ID =====
    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndType(Long userId, NotificationType type);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    // ===== QUERY THEO USER EMAIL (✅ ĐÃ SỬA) =====

    /**
     * Tìm tất cả thông báo của user theo email, sắp xếp theo thời gian gửi giảm dần
     */
    List<Notification> findByUserEmailOrderBySentAtDesc(String email);

    /**
     * Tìm thông báo chưa đọc của user theo email
     */
    List<Notification> findByUserEmailAndIsReadFalseOrderBySentAtDesc(String email);

    /**
     * Tìm thông báo chưa đọc của user theo email (không sắp xếp)
     */
    List<Notification> findByUserEmailAndIsReadFalse(String email);

    /**
     * Đếm số thông báo chưa đọc của user theo email
     */
    long countByUserEmailAndIsReadFalse(String email);

    // ===== QUERY THEO MERCHANT =====

    /**
     * Tìm thông báo theo merchant
     */
    List<Notification> findByMerchantIdOrderBySentAtDesc(Long merchantId);

    /**
     * Tìm thông báo theo merchant email (nếu cần)
     */
    List<Notification> findByMerchantUserEmailOrderBySentAtDesc(String merchantEmail);
}
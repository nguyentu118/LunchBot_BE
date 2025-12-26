package vn.codegym.lunchbot_be.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.model.ReconciliationRequest;
import vn.codegym.lunchbot_be.model.enums.ReconciliationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReconciliationRequestRepository extends JpaRepository<ReconciliationRequest, Long> {
    /**
     * Tìm request theo merchant và tháng
     */
    Optional<ReconciliationRequest> findByMerchantIdAndYearMonth(
            Long merchantId,
            String yearMonth
    );

    /**
     * Kiểm tra đã tồn tại request cho tháng này chưa
     */
    boolean existsByMerchantIdAndYearMonth(
            Long merchantId,
            String yearMonth
    );

    /**
     * Lấy tất cả requests của merchant (phân trang, mới nhất trước)
     */
    Page<ReconciliationRequest> findByMerchantIdOrderByCreatedAtDesc(
            Long merchantId,
            Pageable pageable
    );

    /**
     * Lấy tất cả requests của merchant (không phân trang)
     */
    List<ReconciliationRequest> findByMerchantIdOrderByCreatedAtDesc(
            Long merchantId
    );

    /**
     * Lấy tất cả requests theo status (cho Admin) - phân trang
     */
    Page<ReconciliationRequest> findByStatusOrderByCreatedAtDesc(
            ReconciliationStatus status,
            Pageable pageable
    );

    /**
     * Lấy tất cả requests (cho Admin) - phân trang
     */
    Page<ReconciliationRequest> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    /**
     * Đếm số request PENDING của merchant
     */
    long countByMerchantIdAndStatus(
            Long merchantId,
            ReconciliationStatus status
    );

    /**
     * Đếm tất cả request theo status (cho Admin)
     */
    long countByStatus(ReconciliationStatus status);

    /**
     * Lấy request gần nhất của merchant
     */
    Optional<ReconciliationRequest> findTopByMerchantIdOrderByCreatedAtDesc(
            Long merchantId
    );

    /**
     * Tìm tất cả requests của merchant theo status
     */
    List<ReconciliationRequest> findByMerchantIdAndStatusOrderByCreatedAtDesc(
            Long merchantId,
            ReconciliationStatus status
    );
}

package vn.codegym.lunchbot_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.model.RefundRequest;
import vn.codegym.lunchbot_be.model.enums.RefundStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    // Tìm theo orderId
    Optional<RefundRequest> findByOrderId(Long orderId);

    // Kiểm tra order đã có yêu cầu hoàn tiền chưa
    boolean existsByOrderId(Long orderId);

    // Lấy danh sách theo trạng thái
    List<RefundRequest> findByRefundStatus(RefundStatus status);

    // Lấy danh sách theo trạng thái, sắp xếp theo ngày tạo
    List<RefundRequest> findByRefundStatusOrderByCreatedAtDesc(RefundStatus status);

    // Lấy tất cả, sắp xếp theo ngày tạo
    List<RefundRequest> findAllByOrderByCreatedAtDesc();
}
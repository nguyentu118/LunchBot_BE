package vn.codegym.lunchbot_be.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.codegym.lunchbot_be.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);

    Optional<Address> findByUserIdAndIsDefault(Long userId, Boolean isDefault);

    Long countByUserId(Long userId);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    List<Address> findByUserIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * Lấy địa chỉ theo ID nếu CHƯA XÓA
     */
    @Query("SELECT a FROM Address a WHERE a.id = :id AND a.deletedAt IS NULL")
    Optional<Address> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Lấy địa chỉ mặc định CHƯA XÓA của user
     */
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true AND a.deletedAt IS NULL")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") Long userId);

    /**
     * Đếm số địa chỉ CHƯA XÓA của user
     */
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId AND a.deletedAt IS NULL")
    Long countActiveAddressesByUserId(@Param("userId") Long userId);
}

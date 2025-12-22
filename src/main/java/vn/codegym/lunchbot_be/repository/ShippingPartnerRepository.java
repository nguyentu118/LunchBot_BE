package vn.codegym.lunchbot_be.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.model.ShippingPartner;
import vn.codegym.lunchbot_be.model.enums.ShippingPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingPartnerRepository extends JpaRepository<ShippingPartner, Long> {
    List<ShippingPartner> findByStatus(ShippingPartnerStatus status);

    @Query("SELECT sp FROM ShippingPartner sp WHERE sp.isLocked = false AND sp.status = 'ACTIVE'")
    List<ShippingPartner> findActiveAndUnlocked();

    Long countByStatus(ShippingPartnerStatus status);

    Optional<ShippingPartner> findByIsDefaultTrue();

    @Modifying
    @Transactional
    @Query("UPDATE ShippingPartner sp SET sp.isDefault = false WHERE sp.isDefault = true")
    void resetAllDefaultStatus();

    @Modifying
    @Transactional
    @Query("UPDATE ShippingPartner sp SET sp.isLocked = :isLocked WHERE sp.id = :id")
    void updateLockStatus(@Param("id") Long id, @Param("isLocked") Boolean isLocked);
}
package vn.codegym.lunchbot_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.codegym.lunchbot_be.model.Favorite;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndDishId(Long userId, Long dishId);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.dish d LEFT JOIN FETCH d.images WHERE f.user.id = :userId ORDER BY f.addedAt DESC")
    List<Favorite> findAllByUserIdWithDish(@Param("userId") Long userId);

    boolean existsByUserIdAndDishId(Long userId, Long dishId);

    long countByUserId(Long userId);

    void deleteByUserIdAndDishId(Long userId, Long dishId);

    List<Favorite> findByUserId(Long userId);
}

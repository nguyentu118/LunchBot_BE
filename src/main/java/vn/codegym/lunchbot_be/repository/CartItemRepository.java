package vn.codegym.lunchbot_be.repository;

import vn.codegym.lunchbot_be.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);

    Optional<CartItem> findByCartIdAndDishId(Long cartId, Long dishId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.dish.id = :dishId")
    void deleteByCartIdAndDishId(@Param("cartId") Long cartId, @Param("dishId") Long dishId);

    // Tính tổng số lượng món ăn (quantity) trong giỏ hàng cụ thể
    @Query("SELECT COALESCE(SUM(ci.quantity), 0L) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Long countTotalItemsByCartId(@Param("cartId") Long cartId);
}


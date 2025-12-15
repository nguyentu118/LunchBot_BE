package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.AddToCartRequest;
import vn.codegym.lunchbot_be.dto.response.CartResponse;

public interface CartService {
    Long getCartItemCountByUserEmail(String email);

    void addToCart(String email, AddToCartRequest request);

    CartResponse getCartByUserEmail(String email);

    void updateCartItem(String email, Long dishId, Integer quantity);

    void removeFromCart(String email, Long dishId);

    void clearCart(String email);
}

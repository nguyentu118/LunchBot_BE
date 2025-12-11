package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.model.Cart;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.CartItemRepository;
import vn.codegym.lunchbot_be.repository.CartRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.CartService;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;

    @Override
    public Long getCartItemCountByUserEmail(String email) {
        // 1. Tìm User theo Email để lấy User ID (UserRepository có findByEmail)
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return 0L;
        }
        Long userId = user.getId();

        // 2. Tìm Giỏ hàng (Cart) của User đó (CartRepository có findByUserId)
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        if (cart == null) {
            return 0L;
        }

        // 3. Tính tổng số lượng các món trong Giỏ hàng
        return cartItemRepository.countTotalItemsByCartId(cart.getId());
    }
}
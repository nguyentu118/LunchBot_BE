package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.AddToCartRequest;
import vn.codegym.lunchbot_be.dto.response.CartItemDTO;
import vn.codegym.lunchbot_be.dto.response.CartResponse;
import vn.codegym.lunchbot_be.model.Cart;
import vn.codegym.lunchbot_be.model.CartItem;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.CartItemRepository;
import vn.codegym.lunchbot_be.repository.CartRepository;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.CartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final DishRepository dishRepository;

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

    @Override
    @Transactional
    public void addToCart(String email, AddToCartRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });

        Dish dish = dishRepository.findById(request.getDishId())
                .orElseThrow(() -> new RuntimeException("Dish not found"));

        Optional<CartItem> existingItemOpt = cart.getCartItems().stream()
                .filter(item -> item.getDish().getId().equals(dish.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            // 4a. Nếu có rồi -> Tăng số lượng
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            // 4b. Nếu chưa có -> Tạo CartItem mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .dish(dish)
                    .quantity(request.getQuantity())
                    .price(dish.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        // Cập nhật thời gian sửa đổi của Cart
        cart.setUpdatedAt(java.time.LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElse(null);

        if (cart == null || cart.getCartItems().isEmpty()) {
            return CartResponse.builder()
                    .items(new ArrayList<>())
                    .totalItems(0)
                    .totalPrice(BigDecimal.ZERO)
                    .build();
        }

        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(item -> {
                    // Parse JSON string để lấy URL đầu tiên
                    String firstImage = extractFirstImageUrl(item.getDish().getImagesUrls());

                    return CartItemDTO.builder()
                            .id(item.getId())
                            .dishId(item.getDish().getId())
                            .dishName(item.getDish().getName())
                            .dishImage(firstImage)
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .build();
                })
                .collect(Collectors.toList());

        Integer totalItems = itemDTOs.stream()
                .mapToInt(CartItemDTO::getQuantity)
                .sum();

        BigDecimal totalPrice = itemDTOs.stream()
                .map(CartItemDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .items(itemDTOs)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    // ⭐ MỚI: Cập nhật số lượng
    @Override
    @Transactional
    public void updateCartItem(String email, Long dishId, Integer quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getDish().getId().equals(dishId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        cart.setUpdatedAt(java.time.LocalDateTime.now());
        cartRepository.save(cart);
    }

    // ⭐ MỚI: Xóa món
    @Override
    @Transactional
    public void removeFromCart(String email, Long dishId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getDish().getId().equals(dishId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        cart.setUpdatedAt(java.time.LocalDateTime.now());
        cartRepository.save(cart);
    }

    // ⭐ MỚI: Xóa toàn bộ giỏ
    @Override
    @Transactional
    public void clearCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElse(null);

        if (cart != null) {
            cart.clear();
            cartRepository.save(cart);
        }
    }

    private String extractFirstImageUrl(String imagesUrlsJson) {
        if (imagesUrlsJson == null || imagesUrlsJson.trim().isEmpty()) {
            return null;
        }

        try {
            // Remove brackets and quotes: "[\"url1\",\"url2\"]" → "url1","url2"
            String cleaned = imagesUrlsJson
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .trim();

            String[] urls = cleaned.split(",");
            if (urls.length > 0) {
                return urls[0].trim();
            }
        } catch (Exception e) {
            // If parsing fails, return the original string
            return imagesUrlsJson;
        }

        return null;
    }
}
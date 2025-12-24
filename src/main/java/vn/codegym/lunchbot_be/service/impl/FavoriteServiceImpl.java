package vn.codegym.lunchbot_be.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.response.FavoriteResponse;
import vn.codegym.lunchbot_be.dto.response.FavoriteWithDishResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Dish;
import vn.codegym.lunchbot_be.model.Favorite;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.DishRepository;
import vn.codegym.lunchbot_be.repository.FavoriteRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl {
    private final FavoriteRepository favoriteRepository;

    private final DishRepository dishRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long dishId) {
        log.info("Adding dish {} to favorites for user {}", dishId, userId);

        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Kiểm tra dish tồn tại
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Món ăn không tồn tại"));

        // Kiểm tra đã yêu thích chưa
        if (favoriteRepository.existsByUserIdAndDishId(userId, dishId)) {
            return FavoriteResponse.builder()
                    .success(false)
                    .message("Món ăn đã có trong danh sách yêu thích")
                    .isFavorite(true)
                    .build();
        }

        // Tạo favorite mới
        Favorite favorite = Favorite.builder()
                .user(user)
                .dish(dish)
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);

        return FavoriteResponse.builder()
                .success(true)
                .message("Đã thêm vào danh sách yêu thích")
                .favoriteId(savedFavorite.getId())
                .isFavorite(true)
                .build();
    }

    /**
     * Xóa món ăn khỏi danh sách yêu thích
     */
    @Transactional
    public FavoriteResponse removeFavorite(Long userId, Long dishId) {
        log.info("Removing dish {} from favorites for user {}", dishId, userId);

        // Tìm favorite
        Favorite favorite = favoriteRepository.findByUserIdAndDishId(userId, dishId)
                .orElseThrow(() -> new ResourceNotFoundException("Món ăn không có trong danh sách yêu thích"));

        // Xóa
        favoriteRepository.delete(favorite);

        return FavoriteResponse.builder()
                .success(true)
                .message("Đã xóa khỏi danh sách yêu thích")
                .isFavorite(false)
                .build();
    }

    /**
     * Lấy danh sách yêu thích của user
     */
    @Transactional(readOnly = true)
    public List<Favorite> getUserFavorites(Long userId) {
        log.info("Getting favorites for user {}", userId);
        return favoriteRepository.findAllByUserIdWithDish(userId);
    }

    /**
     * Kiểm tra món ăn có trong danh sách yêu thích không
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long dishId) {
        return favoriteRepository.existsByUserIdAndDishId(userId, dishId);
    }

    /**
     * Toggle favorite (thêm nếu chưa có, xóa nếu đã có)
     */
    @Transactional
    public FavoriteResponse toggleFavorite(Long userId, Long dishId) {
        log.info("Toggling favorite for user {} and dish {}", userId, dishId);

        boolean exists = favoriteRepository.existsByUserIdAndDishId(userId, dishId);

        if (exists) {
            return removeFavorite(userId, dishId);
        } else {
            return addFavorite(userId, dishId);
        }
    }

    /**
     * Đếm số lượng yêu thích của user
     */
    @Transactional(readOnly = true)
    public long countUserFavorites(Long userId) {
        return favoriteRepository.countByUserId(userId);
    }

// Thêm method này vào FavoriteServiceImpl.java

    // Sửa lại method trong FavoriteServiceImpl.java

    @Transactional(readOnly = true)
    public List<FavoriteWithDishResponse> getUserFavoritesWithDish(Long userId) {
        List<Favorite> favorites = favoriteRepository.findAllByUserIdWithDish(userId);

        return favorites.stream()
                .map(favorite -> {
                    Dish dish = favorite.getDish();
                    if (dish == null) return null;

                    // Xử lý parse chuỗi JSON từ cột images_urls
                    List<String> imageList = new ArrayList<>();
                    try {
                        if (dish.getImagesUrls() != null && !dish.getImagesUrls().isEmpty()) {
                            imageList = objectMapper.readValue(
                                    dish.getImagesUrls(),
                                    new TypeReference<List<String>>() {}
                            );
                        }
                    } catch (Exception e) {
                        log.error("Lỗi parse JSON images cho dish {}: {}", dish.getId(), e.getMessage());
                    }

                    return FavoriteWithDishResponse.builder()
                            .id(favorite.getId())
                            .userId(favorite.getUser().getId())
                            .dishId(dish.getId())
                            .createdAt(favorite.getAddedAt())
                            .dish(FavoriteWithDishResponse.DishInfo.builder()
                                    .id(dish.getId())
                                    .name(dish.getName())
                                    .price(dish.getPrice())
                                    .discountPrice(dish.getDiscountPrice())
                                    .merchantName(dish.getMerchant() != null ? dish.getMerchant().getRestaurantName() : "")
                                    .images(imageList) // Trả về danh sách String URL
                                    .build())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}

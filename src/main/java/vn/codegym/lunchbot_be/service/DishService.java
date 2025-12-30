package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.request.DishSearchRequest;
import vn.codegym.lunchbot_be.dto.response.DishDetailResponse;
import vn.codegym.lunchbot_be.dto.response.DishDiscountResponse;
import vn.codegym.lunchbot_be.dto.response.DishSearchResponse;
import vn.codegym.lunchbot_be.dto.response.SuggestedDishResponse;
import vn.codegym.lunchbot_be.model.Dish;

import java.math.BigDecimal;
import java.util.List;

@Service
public interface DishService {
    Dish createNewDish(DishCreateRequest request, String username);

    List<Dish> findAllDishesByMerchantUsername(String username);

    Dish findDishById(Long dishId);

    Dish updateDish(Long dishId, DishCreateRequest request, String username);

    void deleteDish(Long dishId, String username);

    DishDetailResponse getDishDetail(Long dishId);

    List<SuggestedDishResponse> getTopSuggestedDishes();

    List<SuggestedDishResponse> getRelatedDishesByCategory(Long dishId);

    List<SuggestedDishResponse> getMostViewedDishes();
    List<DishDiscountResponse> getTop8MostDiscountedDishes();

    Page<Dish> searchDishes(Long merchantId, String keyword, Long categoryId,
                            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    List<DishSearchResponse> quickSearchDishes(String name, String category);

    Page<DishSearchResponse> searchDishes(DishSearchRequest request);

    Page<DishDiscountResponse> getAllDiscountedDishesWithPagination(
            String keyword,
            String sortBy,
            Pageable pageable
    );

    Page<SuggestedDishResponse> getAllSuggestedDishesWithPagination(
            String keyword,
            String sortBy,
            Pageable pageable
    );
}

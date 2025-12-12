package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.response.DishDetailResponse;
import vn.codegym.lunchbot_be.dto.response.SuggestedDishResponse;
import vn.codegym.lunchbot_be.model.Dish;

import java.util.List;

public interface DishService {
    Dish createNewDish(DishCreateRequest request, String username);

    List<Dish> findAllDishesByMerchantUsername(String username);

    Dish findDishById(Long dishId);

    Dish updateDish(Long dishId, DishCreateRequest request, String username);

    void deleteDish(Long dishId, String username);

    DishDetailResponse getDishDetail(Long dishId);

    List<SuggestedDishResponse> getTopSuggestedDishes();
}

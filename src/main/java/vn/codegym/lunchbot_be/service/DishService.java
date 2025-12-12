package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.dto.response.SuggestedDishResponse;
import vn.codegym.lunchbot_be.model.Dish;

import java.util.List;

public interface DishService {
    // CREATE
    Dish createNewDish(DishCreateRequest request, String username);

    // READ (List)
    List<Dish> findAllDishesByMerchantUsername(String username);

    // READ (Detail by ID) - HỖ TRỢ FE LẤY DỮ LIỆU CŨ
    Dish findDishById(Long dishId);

    // UPDATE
    Dish updateDish(Long dishId, DishCreateRequest request, String username);

    // DELETE
    void deleteDish(Long dishId, String username);

    List<SuggestedDishResponse> getTopSuggestedDishes();
}

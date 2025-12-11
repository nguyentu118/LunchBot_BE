package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.DishCreateRequest;
import vn.codegym.lunchbot_be.model.Dish;

import java.util.List;

public interface DishService {
    List<Dish> findAllDishesByMerchantUsername(String username);

    Dish createNewDish(DishCreateRequest request, String username);
}

package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.response.DishResponse;
import vn.codegym.lunchbot_be.dto.response.MerchantProfileResponse;
import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;
import vn.codegym.lunchbot_be.model.Merchant;

import java.util.List;
import java.util.Optional;

public interface MerchantService {
    List<PopularMerchantDto> getPopularMerchants(int limit);

    MerchantProfileResponse getMerchantById(Long id);

    void updateMerchantAvatar(Long userId, String avatarUrl);

    Merchant findByUserId(Long userId);

    List<DishResponse> getDishesByMerchantId(Long merchantId);
}
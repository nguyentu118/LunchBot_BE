package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;

import java.util.List;

public interface MerchantService {
    List<PopularMerchantDto> getPopularMerchants(int limit);
}
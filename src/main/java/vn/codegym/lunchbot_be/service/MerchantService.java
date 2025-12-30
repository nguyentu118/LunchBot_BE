package vn.codegym.lunchbot_be.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.codegym.lunchbot_be.dto.response.DishResponse;
import vn.codegym.lunchbot_be.dto.response.MerchantProfileResponse;
import vn.codegym.lunchbot_be.dto.response.MerchantResponseDTO;
import vn.codegym.lunchbot_be.dto.response.PopularMerchantDto;
import vn.codegym.lunchbot_be.model.Coupon;
import vn.codegym.lunchbot_be.model.Merchant;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MerchantService {
    List<PopularMerchantDto> getPopularMerchants(int limit);

    MerchantProfileResponse getMerchantById(Long id);

    void updateMerchantAvatar(Long userId, String avatarUrl);

    Merchant findByUserId(Long userId);

    List<DishResponse> getDishesByMerchantId(Long merchantId);
    // ...
    void registerPartner(Long merchantId);

    public BigDecimal calculateCurrentMonthRevenue(Long merchantId);

    // Admin: Lấy danh sách chờ duyệt
    List<MerchantProfileResponse> getPendingPartnerRequests();

    // Admin: Duyệt yêu cầu
    void approvePartnerRequest(Long merchantId);

    // Admin: Từ chối yêu cầu
    void rejectPartnerRequest(Long merchantId, String reason);

    Page<MerchantResponseDTO> getAllMerchantsWithPagination(Pageable pageable, String status);

}
// service/impl/AdminMerchantServiceImpl.java
package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.MerchantApprovalRequest;
import vn.codegym.lunchbot_be.dto.request.MerchantLockRequest;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantListResponse;
import vn.codegym.lunchbot_be.dto.response.AdminMerchantResponse;
import vn.codegym.lunchbot_be.dto.response.DishSimpleResponse;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.enums.MerchantStatus;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.OrderRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;
import vn.codegym.lunchbot_be.service.AdminMerchantService;
import vn.codegym.lunchbot_be.service.EmailService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminMerchantServiceImpl implements AdminMerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Override
    public Page<AdminMerchantListResponse> getAllMerchants(Pageable pageable) {
        Page<Merchant> merchants = merchantRepository.findAll(pageable);
        return merchants.map(this::convertToAdminMerchantListResponse);
    }

    @Override
    public Page<AdminMerchantListResponse> getMerchantsByStatus(MerchantStatus status, Pageable pageable) {
        Page<Merchant> merchants = merchantRepository.findByStatus(status, pageable);
        return merchants.map(this::convertToAdminMerchantListResponse);
    }

    @Override
    public Page<AdminMerchantListResponse> searchMerchants(String keyword, Pageable pageable) {
        Page<Merchant> merchants = merchantRepository.searchMerchants(keyword, pageable);
        return merchants.map(this::convertToAdminMerchantListResponse);
    }

    @Override
    public AdminMerchantResponse getMerchantDetails(Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + merchantId));

        return convertToAdminMerchantResponse(merchant);
    }

    @Override
    @Transactional
    public AdminMerchantResponse approveMerchant(Long merchantId, MerchantApprovalRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + merchantId));

        if (request.getApproved()) {
            // Duyệt merchant
            merchant.approve(request.getReason());

            // Gửi email thông báo duyệt
            emailService.sendMerchantApprovalEmail(
                    merchant.getUser().getEmail(),
                    merchant.getUser().getFullName(),
                    merchant.getRestaurantName(),
                    request.getReason()
            );

            log.info("Merchant {} approved by admin", merchantId);
        } else {
            // Từ chối merchant
            merchant.reject(request.getReason());

            // Gửi email thông báo từ chối
            emailService.sendMerchantRejectionEmail(
                    merchant.getUser().getEmail(),
                    merchant.getUser().getFullName(),
                    merchant.getRestaurantName(),
                    request.getReason()
            );

            log.info("Merchant {} rejected by admin", merchantId);
        }

        merchantRepository.save(merchant);
        userRepository.save(merchant.getUser());

        return convertToAdminMerchantResponse(merchant);
    }

    @Override
    @Transactional
    public AdminMerchantResponse lockUnlockMerchant(Long merchantId, MerchantLockRequest request) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + merchantId));

        if (request.getLock()) {
            // Khóa merchant
            merchant.lock(request.getReason());

            // Gửi email thông báo khóa
            emailService.sendMerchantLockedEmail(
                    merchant.getUser().getEmail(),
                    merchant.getUser().getFullName(),
                    merchant.getRestaurantName(),
                    request.getReason()
            );

            log.info("Merchant {} locked by admin", merchantId);
        } else {
            // Mở khóa merchant
            merchant.unlock(request.getReason());

            // Gửi email thông báo mở khóa
            emailService.sendMerchantUnlockedEmail(
                    merchant.getUser().getEmail(),
                    merchant.getUser().getFullName(),
                    merchant.getRestaurantName(),
                    request.getReason()
            );

            log.info("Merchant {} unlocked by admin", merchantId);
        }

        merchantRepository.save(merchant);
        userRepository.save(merchant.getUser());

        return convertToAdminMerchantResponse(merchant);
    }

    @Override
    public Long countPendingMerchants() {
        return merchantRepository.countPendingMerchants();
    }

    @Override
    public Long countLockedMerchants() {
        return merchantRepository.countLockedMerchants();
    }

    @Override
    public Long countApprovedMerchants() {
        return merchantRepository.countApprovedMerchants();
    }

    // Helper methods
    private AdminMerchantListResponse convertToAdminMerchantListResponse(Merchant merchant) {
        AdminMerchantListResponse response = new AdminMerchantListResponse();

        response.setId(merchant.getId());
        response.setRestaurantName(merchant.getRestaurantName());
        response.setOwnerName(merchant.getUser().getFullName());
        response.setEmail(merchant.getUser().getEmail());
        response.setPhone(merchant.getPhone());
        response.setStatus(merchant.getStatus());
        response.setIsLocked(merchant.getIsLocked());
        response.setIsApproved(merchant.getIsApproved());
        response.setRevenueTotal(merchant.getRevenueTotal());
        response.setCurrentBalance(merchant.getCurrentBalance());
        response.setRegistrationDate(merchant.getRegistrationDate());
        response.setDishCount(merchant.getDishes().size());

        // Tính số đơn hàng
        Long orderCount = orderRepository.countByMerchantId(merchant.getId());
        response.setOrderCount(orderCount != null ? orderCount.intValue() : 0);

        return response;
    }

    private AdminMerchantResponse convertToAdminMerchantResponse(Merchant merchant) {
        AdminMerchantResponse response = new AdminMerchantResponse();

        response.setId(merchant.getId());
        response.setRestaurantName(merchant.getRestaurantName());
        response.setOwnerName(merchant.getUser().getFullName());
        response.setEmail(merchant.getUser().getEmail());
        response.setPhone(merchant.getPhone());
        response.setAddress(merchant.getAddress());
        response.setOpenTime(merchant.getOpenTime());
        response.setCloseTime(merchant.getCloseTime());
        response.setRevenueTotal(merchant.getRevenueTotal());
        response.setCurrentBalance(merchant.getCurrentBalance());
        response.setStatus(merchant.getStatus());
        response.setIsPartner(merchant.getIsPartner());
        response.setIsLocked(merchant.getIsLocked());
        response.setIsApproved(merchant.getIsApproved());
        response.setRejectionReason(merchant.getRejectionReason());
        response.setRegistrationDate(merchant.getRegistrationDate());
        response.setApprovalDate(merchant.getApprovalDate());
        response.setLockedAt(merchant.getLockedAt());
        response.setDishCount(merchant.getDishes().size());

        // Thống kê đơn hàng
        Long totalOrders = orderRepository.countByMerchantId(merchant.getId());
        Long completedOrders = orderRepository.countByMerchantIdAndStatus(merchant.getId(),
                vn.codegym.lunchbot_be.model.enums.OrderStatus.COMPLETED);
        Long cancelledOrders = orderRepository.countByMerchantIdAndStatus(merchant.getId(),
                vn.codegym.lunchbot_be.model.enums.OrderStatus.CANCELLED);

        response.setTotalOrders(totalOrders != null ? totalOrders : 0L);
        response.setCompletedOrders(completedOrders != null ? completedOrders : 0L);
        response.setCancelledOrders(cancelledOrders != null ? cancelledOrders : 0L);

        // Doanh thu tháng
        BigDecimal monthlyRevenue = orderRepository.getMonthlyRevenue(merchant.getId(),
                LocalDateTime.now().getMonthValue(),
                LocalDateTime.now().getYear());
        response.setMonthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO);

        // Danh sách món ăn (cho task 27)
        response.setDishes(merchant.getDishes().stream()
                .map(dish -> {
                    DishSimpleResponse dishResponse = new DishSimpleResponse();
                    dishResponse.setId(dish.getId());
                    dishResponse.setName(dish.getName());
                    dishResponse.setDescription(dish.getDescription());
                    dishResponse.setPrice(dish.getPrice());
                    dishResponse.setDiscountPrice(dish.getDiscountPrice());
                    dishResponse.setIsActive(dish.getIsActive());
                    dishResponse.setViewCount(dish.getViewCount());
                    dishResponse.setOrderCount(dish.getOrderCount());
                    return dishResponse;
                })
                .collect(Collectors.toList()));

        return response;
    }
}

package vn.codegym.lunchbot_be.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.exception.InvalidOperationException;
import vn.codegym.lunchbot_be.exception.ResourceNotFoundException;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.model.User;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.repository.UserRepository;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl {

    private final UserRepository userRepository;

    private final MerchantRepository merchantRepository;

    @Transactional
    public Merchant updateMerchanntInfo(Long userId, MerchantUpdateRequest request) {

        LocalTime openTime;
        LocalTime closeTime;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        Merchant merchant = user.getMerchant();

        if (merchant == null) {
            throw new ResourceNotFoundException("Merchant không tồn tại");
        }

        try {
            openTime = LocalTime.parse(request.getOpenTime());
            closeTime = LocalTime.parse(request.getCloseTime());

        } catch (DateTimeParseException e) {
            throw new InvalidOperationException("Định dạng giờ mở/đóng không hợp lệ. Vui lòng sử dụng định dạng HH:mm:ss.");
        }
        if (closeTime.isBefore(openTime) || closeTime.equals(openTime)) {
            throw new InvalidOperationException("Giờ đóng cửa phải sau giờ mở cửa.");
        }

        merchant.setRestaurantName(request.getRestaurantName());
        merchant.setAddress(request.getAddress());
        merchant.setOpenTime(openTime);
        merchant.setCloseTime(closeTime);

        return merchantRepository.save(merchant);

    }

}

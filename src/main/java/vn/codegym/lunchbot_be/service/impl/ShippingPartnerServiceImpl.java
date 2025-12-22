package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.codegym.lunchbot_be.dto.request.ShippingPartnerRequest;
import vn.codegym.lunchbot_be.model.ShippingPartner;
import vn.codegym.lunchbot_be.model.enums.ShippingPartnerStatus;
import vn.codegym.lunchbot_be.repository.ShippingPartnerRepository;
import vn.codegym.lunchbot_be.service.EmailService;
import vn.codegym.lunchbot_be.service.ShippingPartnerService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingPartnerServiceImpl implements ShippingPartnerService {

    private final ShippingPartnerRepository shippingPartnerRepository;

    private final EmailService emailService;

    @Override
    @Transactional
    public ShippingPartner createPartner(ShippingPartnerRequest request) {
        boolean isDefaultValue = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefaultValue) {
            shippingPartnerRepository.resetAllDefaultStatus();
            shippingPartnerRepository.flush();
        }

        ShippingPartner partner = ShippingPartner.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .commissionRate(request.getCommissionRate())
                .isDefault(isDefaultValue)
                .status(ShippingPartnerStatus.ACTIVE)
                .isLocked(false)
                .build();

        ShippingPartner saved = shippingPartnerRepository.save(partner);

        verifyOnlyOneDefault();

        return saved;
    }

    @Override
    public List<ShippingPartner> getAllPartners() {
        return shippingPartnerRepository.findAll();
    }

    @Override
    public ShippingPartner getPartnerById(Long id) {
        return shippingPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác với ID: " + id));
    }

    @Override
    @Transactional
    public ShippingPartner updatePartner(Long id, ShippingPartnerRequest request) {
        ShippingPartner existingPartner = shippingPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác vận chuyển với ID: " + id));

        boolean isDefaultValue = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefaultValue) {
            shippingPartnerRepository.resetAllDefaultStatus();
            // Flush để đảm bảo reset được thực thi ngay lập tức
            shippingPartnerRepository.flush();
        }

        existingPartner.setName(request.getName());
        existingPartner.setEmail(request.getEmail());
        existingPartner.setPhone(request.getPhone());
        existingPartner.setAddress(request.getAddress());
        existingPartner.setCommissionRate(request.getCommissionRate());

        existingPartner.setIsDefault(isDefaultValue);

        ShippingPartner updated = shippingPartnerRepository.save(existingPartner);

        verifyOnlyOneDefault();

        return updated;
    }

    @Override
    @Transactional
    public void toggleLock(Long id, String reason) {
        ShippingPartner partner = getPartnerById(id);

        Boolean wasLocked = partner.getIsLocked();

        // Đảo trạng thái
        partner.setIsLocked(!wasLocked);
        ShippingPartner updated = shippingPartnerRepository.save(partner);

        // ✅ GỬI EMAIL VÀO THAO TÁC
        if (!wasLocked) {
            // Thay đổi từ KHÔNG khóa → KHÓA
            emailService.sendShippingPartnerLockedEmail(
                    partner.getEmail(),
                    partner.getName(),
                    reason != null && !reason.trim().isEmpty()
                            ? reason
                            : "Vi phạm chính sách dịch vụ"  // Default nếu không có lý do
            );
        } else {
            // Thay đổi từ KHÓA → KHÔNG khóa
            emailService.sendShippingPartnerUnlockedEmail(
                    partner.getEmail(),
                    partner.getName(),
                    reason != null && !reason.trim().isEmpty()
                            ? reason
                            : "Tài khoản đã được xác minh lại"  // Default nếu không có lý do
            );
        }
    }

    private void verifyOnlyOneDefault() {
        List<ShippingPartner> defaultPartners = shippingPartnerRepository.findAll()
                .stream()
                .filter(ShippingPartner::getIsDefault)
                .toList();

        if (defaultPartners.size() > 1) {

            for (int i = 1; i < defaultPartners.size(); i++) {
                ShippingPartner partner = defaultPartners.get(i);
                partner.setIsDefault(false);
                shippingPartnerRepository.save(partner);
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultPartner(Long id) {
        ShippingPartner partner = shippingPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đối tác với ID: " + id));

        shippingPartnerRepository.resetAllDefaultStatus();

        partner.setIsDefault(true);
        shippingPartnerRepository.save(partner);
    }


}
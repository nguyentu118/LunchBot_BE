package vn.codegym.lunchbot_be.service;

import vn.codegym.lunchbot_be.dto.request.ShippingPartnerRequest;
import vn.codegym.lunchbot_be.model.ShippingPartner;

import java.util.List;

public interface ShippingPartnerService {

    ShippingPartner createPartner(ShippingPartnerRequest request);

    List<ShippingPartner> getAllPartners();

    ShippingPartner getPartnerById(Long id);

    ShippingPartner updatePartner(Long id, ShippingPartnerRequest request);

    void toggleLock(Long id, String reason);

    void setDefaultPartner(Long id);

}

package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.codegym.lunchbot_be.dto.request.GhnFeeRequest;
import vn.codegym.lunchbot_be.dto.request.GhnServiceRequest;
import vn.codegym.lunchbot_be.model.Address;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl {

    @Value("${ghn.api.token}")
    private String GHN_TOKEN;

    @Value("${ghn.shop.id}")
    private Integer GHN_SHOP_ID;

    @Value("${ghn.shop.from-district-id:1454}")
    private Integer GHN_FROM_DISTRICT_ID;

    @Value("${ghn.default.shipping-fee:25000}")
    private Long DEFAULT_SHIPPING_FEE;

    private final String GHN_SERVICES_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/available-services";

    private final String GHN_FEE_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee";

    private final RestTemplate restTemplate;

    public Long calculateGhnFee(Address address) {
        if (address.getDistrictId() == null || address.getWardCode() == null) {
            return DEFAULT_SHIPPING_FEE;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", GHN_TOKEN);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Integer serviceId = getValidServiceId(address.getDistrictId(), headers);
            if (serviceId == null) {
                serviceId = 53320;
            }

            // BƯỚC 2: Tạo request DTO
            GhnFeeRequest feeRequest = GhnFeeRequest.builder()
                    .fromDistrictId(GHN_FROM_DISTRICT_ID)
                    .toDistrictId(address.getDistrictId())
                    .toWardCode(address.getWardCode())
                    .serviceId(serviceId)
                    .weight(500)
                    .length(15)
                    .width(15)
                    .height(10)
                    .insuranceValue(0)
                    .build();

            HttpEntity<GhnFeeRequest> entity = new HttpEntity<>(feeRequest, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GHN_FEE_URL, HttpMethod.POST, entity, Map.class);


            if (response.getBody() != null) {
                Integer code = parseCode(response.getBody().get("code"));

                if (code != null && code == 200) {
                    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                    Long fee = Long.valueOf(data.get("total").toString());
                    return fee;
                } else {
                    System.err.println("❌ GHN API error: " + response.getBody().get("message"));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối GHN API: " + e.getMessage());
            e.printStackTrace();
        }
        return DEFAULT_SHIPPING_FEE;
    }

    private Integer getValidServiceId(Integer toDistrictId, HttpHeaders headers) {
        try {

            GhnServiceRequest serviceRequest = GhnServiceRequest.builder()
                    .shopId(GHN_SHOP_ID)
                    .fromDistrict(GHN_FROM_DISTRICT_ID)
                    .toDistrict(toDistrictId)
                    .build();

            HttpEntity<GhnServiceRequest> entity = new HttpEntity<>(serviceRequest, headers);
            ResponseEntity<Map> response = restTemplate.exchange(GHN_SERVICES_URL, HttpMethod.POST, entity, Map.class);

            if (response.getBody() != null && parseCode(response.getBody().get("code")) == 200) {
                List<Map<String, Object>> services = (List<Map<String, Object>>) response.getBody().get("data");
                if (services != null && !services.isEmpty()) {
                    Integer serviceId = (Integer) services.get(0).get("service_id");
                    return serviceId;
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không lấy được service list: " + e.getMessage());
        }
        return null;
    }

    private Integer parseCode(Object codeObj) {
        if (codeObj instanceof Integer) return (Integer) codeObj;
        if (codeObj instanceof Double) return ((Double) codeObj).intValue();
        return null;
    }
}
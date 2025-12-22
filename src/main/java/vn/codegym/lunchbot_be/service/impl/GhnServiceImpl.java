package vn.codegym.lunchbot_be.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GhnServiceImpl {

    @Value("${ghn.api.token}")
    private String GHN_TOKEN;

    @Value("${ghn.shop.id}")
    private Integer GHN_SHOP_ID;

    // ✅ GHN API v2 endpoints (đúng)
    private final String GHN_BASE_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api";

    private final RestTemplate restTemplate;

    /**
     * Lấy danh sách tất cả tỉnh/thành phố từ GHN
     * ✅ Endpoint: /master-data/province
     */
    public List<Map<String, Object>> getProvinces() {
        String url = GHN_BASE_URL + "/master-data/province";

        try {
            Map<String, Object> body = new HashMap<>();

            HttpHeaders headers = createGhnHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null) {
                Object codeObj = responseBody.get("code");
                Integer code = parseCode(codeObj);

                if (code != null && code == 200) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    return data != null ? data : List.of();
                } else {
                    String message = (String) responseBody.get("message");
                    throw new RuntimeException("GHN API Error (code " + code + "): " + message);
                }
            }

            throw new RuntimeException("GHN API trả về response null");
        } catch (Exception e) {
            System.err.println("❌ Error fetching provinces: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lấy danh sách tỉnh/thành phố: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     * ✅ Endpoint: /master-data/district
     */
    public List<Map<String, Object>> getDistrictsByProvince(Integer provinceId) {
        String url = GHN_BASE_URL + "/master-data/district";

        try {

            Map<String, Object> body = new HashMap<>();
            body.put("province_id", provinceId);

            HttpHeaders headers = createGhnHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null) {
                Object codeObj = responseBody.get("code");
                Integer code = parseCode(codeObj);

                if (code != null && code == 200) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    return data != null ? data : List.of();
                } else {
                    String message = (String) responseBody.get("message");
                    throw new RuntimeException("GHN API Error: " + message);
                }
            }

            throw new RuntimeException("GHN API trả về response null");
        } catch (Exception e) {
            System.err.println("❌ Error fetching districts: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách quận/huyện: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách phường/xã theo quận
     * ✅ Endpoint: /master-data/ward
     */
    public List<Map<String, Object>> getWardsByDistrict(Integer districtId) {
        String url = GHN_BASE_URL + "/master-data/ward";

        try {

            Map<String, Object> body = new HashMap<>();
            body.put("district_id", districtId);

            HttpHeaders headers = createGhnHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null) {
                Object codeObj = responseBody.get("code");
                Integer code = parseCode(codeObj);

                if (code != null && code == 200) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");
                    return data != null ? data : List.of();
                } else {
                    String message = (String) responseBody.get("message");
                    throw new RuntimeException("GHN API Error: " + message);
                }
            }

            throw new RuntimeException("GHN API trả về response null");
        } catch (Exception e) {
            System.err.println("❌ Error fetching wards: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách phường/xã: " + e.getMessage());
        }
    }

    /**
     * Lấy thông tin chi tiết một quận
     */
    public Map<String, Object> getDistrictById(Integer districtId) {
        throw new RuntimeException("Sử dụng getDistrictsByProvince() rồi filter");
    }

    /**
     * Lấy thông tin chi tiết một phường
     */
    public Map<String, Object> getWardByCode(String wardCode) {
        throw new RuntimeException("Sử dụng getWardsByDistrict() rồi filter theo wardCode");
    }

    /**
     * ✅ Tạo GHN headers đúng
     */
    private HttpHeaders createGhnHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", GHN_TOKEN);
        headers.set("ShopId", GHN_SHOP_ID.toString());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    /**
     * Parse code từ response (có thể là Integer hoặc Double)
     */
    private Integer parseCode(Object codeObj) {
        if (codeObj instanceof Integer) {
            return (Integer) codeObj;
        } else if (codeObj instanceof Double) {
            return ((Double) codeObj).intValue();
        }
        return null;
    }
}
package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.service.impl.GhnServiceImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ghn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GhnController {

    private final GhnServiceImpl ghnService;

    /**
     * Lấy danh sách tất cả tỉnh/thành phố
     */
    @GetMapping("/provinces")
    public ResponseEntity<List<Map<String, Object>>> getProvinces() {
        try {
            List<Map<String, Object>> provinces = ghnService.getProvinces();
            return ResponseEntity.ok(provinces);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh
     * @param provinceId - ID tỉnh từ GHN
     */
    @GetMapping("/districts")
    public ResponseEntity<List<Map<String, Object>>> getDistricts(
            @RequestParam Integer provinceId) {
        try {
            List<Map<String, Object>> districts = ghnService.getDistrictsByProvince(provinceId);
            return ResponseEntity.ok(districts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Lấy danh sách phường/xã theo quận
     * @param districtId - ID quận từ GHN
     */
    @GetMapping("/wards")
    public ResponseEntity<List<Map<String, Object>>> getWards(
            @RequestParam Integer districtId) {
        try {
            List<Map<String, Object>> wards = ghnService.getWardsByDistrict(districtId);
            return ResponseEntity.ok(wards);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Lấy thông tin chi tiết một quận
     */
    @GetMapping("/districts/{districtId}")
    public ResponseEntity<Map<String, Object>> getDistrictById(
            @PathVariable Integer districtId) {
        try {
            Map<String, Object> district = ghnService.getDistrictById(districtId);
            return ResponseEntity.ok(district);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Lấy thông tin chi tiết một phường
     */
    @GetMapping("/wards/{wardCode}")
    public ResponseEntity<Map<String, Object>> getWardByCode(
            @PathVariable String wardCode) {
        try {
            Map<String, Object> ward = ghnService.getWardByCode(wardCode);
            return ResponseEntity.ok(ward);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
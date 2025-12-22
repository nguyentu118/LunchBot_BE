package vn.codegym.lunchbot_be.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.model.Address;
import vn.codegym.lunchbot_be.repository.AddressRepository;
import vn.codegym.lunchbot_be.service.impl.ShippingServiceImpl;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingServiceImpl shippingService;

    private final AddressRepository addressRepository;

    @GetMapping("/calculate-fee")
    public ResponseEntity<?> getShippingFee(@RequestParam Long addressId) {
        try {
            // Tìm địa chỉ trong DB
            Address address = addressRepository.findById(addressId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ với ID: " + addressId));

            // Gọi service để tính phí từ GHN
            Long fee = shippingService.calculateGhnFee(address);

            // Trả về phí cho Frontend
            return ResponseEntity.ok(fee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi khi tính phí: " + e.getMessage());
        }
    }
}

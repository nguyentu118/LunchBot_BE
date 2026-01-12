package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.ShippingPartnerRequest;
import vn.codegym.lunchbot_be.dto.response.ApiResponse;
import vn.codegym.lunchbot_be.model.ShippingPartner;
import vn.codegym.lunchbot_be.service.ShippingPartnerService;

import java.util.List;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class ShippingPartnerController {
    private final ShippingPartnerService shippingPartnerService;

    @PostMapping("/create")
    public ResponseEntity<ShippingPartner> createShippingPartner(@Valid @RequestBody ShippingPartnerRequest request) {
        ShippingPartner newPartner = shippingPartnerService.createPartner(request);
        return new ResponseEntity<>(newPartner, HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ShippingPartner>> getAll() {
        List<ShippingPartner> partners = shippingPartnerService.getAllPartners();
        return ResponseEntity.ok(partners);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShippingPartner> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shippingPartnerService.getPartnerById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingPartner> update(
            @PathVariable Long id,
            @Valid @RequestBody ShippingPartnerRequest request) {

        ShippingPartner updatedPartner = shippingPartnerService.updatePartner(id, request);
        return ResponseEntity.ok(updatedPartner);
    }

    @PatchMapping("/{id}/toggle-lock")
    public ResponseEntity<?> toggleLock(
            @PathVariable Long id,
            @RequestBody ShippingPartnerRequest request) {  // @Valid để trigger validation
        try {
            // ✅ Validate lockReason không trống
            if (request.getLockReason() == null || request.getLockReason().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Lý do khóa không được để trống"));
            }

            String lockReason = request.getLockReason().trim();
            shippingPartnerService.toggleLock(id, lockReason);

            return ResponseEntity.ok(new ApiResponse(true, "Cập nhật trạng thái khóa thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/set-default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id) {
        shippingPartnerService.setDefaultPartner(id);
        return ResponseEntity.ok().build();
    }
}

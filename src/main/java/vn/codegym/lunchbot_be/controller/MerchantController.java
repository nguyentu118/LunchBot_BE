package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.model.Merchant;
import vn.codegym.lunchbot_be.repository.MerchantRepository;
import vn.codegym.lunchbot_be.service.impl.MerchantServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;

    private final MerchantServiceImpl merchantService;

    @PutMapping("/profile")
    public ResponseEntity<Merchant> updateMerchantInfo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MerchantUpdateRequest request
            ) {
        Long UserId = userDetails.getId();
        Merchant updatedMerchant = merchantService.updateMerchanntInfo(UserId, request);
        return ResponseEntity.ok(updatedMerchant);
    }

}

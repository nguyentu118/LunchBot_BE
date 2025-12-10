package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.MerchantUpdateRequest;
import vn.codegym.lunchbot_be.dto.response.MerchantResponseDTO;
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

    @GetMapping("/profile")
    // Authentication object được inject tự động bởi Spring Security
    public ResponseEntity<MerchantResponseDTO> getMerchantProfile(Authentication authentication) {
        // 1. Lấy email của Merchant đang đăng nhập từ Security Context
        String merchantEmail = authentication.getName();

        // 2. Gọi Service để lấy thông tin chi tiết
        MerchantResponseDTO profile = merchantService.getMerchantProfileByEmail(merchantEmail);

        return ResponseEntity.ok(profile);
    }

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

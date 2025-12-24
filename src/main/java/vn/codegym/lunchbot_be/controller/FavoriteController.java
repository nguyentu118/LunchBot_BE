package vn.codegym.lunchbot_be.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.codegym.lunchbot_be.dto.request.FavoriteRequest;
import vn.codegym.lunchbot_be.dto.response.FavoriteResponse;
import vn.codegym.lunchbot_be.dto.response.FavoriteWithDishResponse;
import vn.codegym.lunchbot_be.model.Favorite;
import vn.codegym.lunchbot_be.service.impl.FavoriteServiceImpl;
import vn.codegym.lunchbot_be.service.impl.UserDetailsImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteController {
    private final FavoriteServiceImpl favoriteService;

    @GetMapping
    public ResponseEntity<List<FavoriteWithDishResponse>> getUserFavorites(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();
        List<FavoriteWithDishResponse> favorites = favoriteService.getUserFavoritesWithDish(userId);

        return ResponseEntity.ok(favorites);
    }

    @PostMapping
    public ResponseEntity<FavoriteResponse> addFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FavoriteRequest request) {

        Long userId = userDetails.getId();
        FavoriteResponse response = favoriteService.addFavorite(userId, request.getDishId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{dishId}")
    public ResponseEntity<FavoriteResponse> removeFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long dishId) {

        Long userId = userDetails.getId();
        FavoriteResponse response = favoriteService.removeFavorite(userId, dishId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/toggle")
    public ResponseEntity<FavoriteResponse> toggleFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody FavoriteRequest request) {

        Long userId = userDetails.getId();
        FavoriteResponse response = favoriteService.toggleFavorite(userId, request.getDishId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{dishId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long dishId) {

        Long userId = userDetails.getId();
        boolean isFavorite = favoriteService.isFavorite(userId, dishId);

        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countFavorites(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long userId = userDetails.getId();
        long count = favoriteService.countUserFavorites(userId);

        return ResponseEntity.ok(Map.of("count", count));
    }
}

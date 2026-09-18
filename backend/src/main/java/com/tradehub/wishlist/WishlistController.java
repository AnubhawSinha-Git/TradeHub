package com.tradehub.wishlist;

import com.tradehub.wishlist.dto.AddWishlistItemRequest;
import com.tradehub.wishlist.dto.WishlistItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(
            Authentication authentication) {
        return ResponseEntity.ok(wishlistService.getWishlist(authentication.getName()));
    }

    @PostMapping("/items")
    public ResponseEntity<List<WishlistItemResponse>> addItem(
            Authentication authentication,
            @Valid @RequestBody AddWishlistItemRequest request) {
        List<WishlistItemResponse> response =
                wishlistService.addItem(authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/items/{wishlistItemId}")
    public ResponseEntity<List<WishlistItemResponse>> removeItem(
            Authentication authentication,
            @PathVariable Long wishlistItemId) {
        List<WishlistItemResponse> response =
                wishlistService.removeItem(authentication.getName(), wishlistItemId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearWishlist(Authentication authentication) {
        wishlistService.clearWishlist(authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
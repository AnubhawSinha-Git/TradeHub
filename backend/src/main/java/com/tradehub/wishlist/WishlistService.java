package com.tradehub.wishlist;

import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import com.tradehub.wishlist.dto.AddWishlistItemRequest;
import com.tradehub.wishlist.dto.WishlistItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(
            WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<WishlistItemResponse> getWishlist(String email) {
        User user = findUser(email);

        return wishlistItemRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WishlistItemResponse> addItem(
            String email,
            AddWishlistItemRequest request) {
        User user = findUser(email);

        if (wishlistItemRepository
                .existsByUserIdAndProductId(user.getId(), request.productId())) {
            throw new IllegalArgumentException("Product is already in the wishlist");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistItemRepository.save(item);

        return getWishlist(email);
    }

    @Transactional
    public List<WishlistItemResponse> removeItem(String email, Long wishlistItemId) {
        User user = findUser(email);

        WishlistItem item = wishlistItemRepository.findById(wishlistItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Wishlist item not found"));

        if (!item.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Wishlist item does not belong to the user");
        }

        wishlistItemRepository.delete(item);

        return getWishlist(email);
    }

    @Transactional
    public void clearWishlist(String email) {
        User user = findUser(email);
        wishlistItemRepository.deleteByUserId(user.getId());
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private WishlistItemResponse toResponse(WishlistItem item) {
        Product product = item.getProduct();

        return new WishlistItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getPrice(),
                product.isActive(),
                item.getCreatedAt());
    }
}
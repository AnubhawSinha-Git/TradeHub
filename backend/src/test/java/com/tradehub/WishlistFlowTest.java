package com.tradehub;

import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import com.tradehub.wishlist.WishlistService;
import com.tradehub.wishlist.dto.AddWishlistItemRequest;
import com.tradehub.wishlist.dto.WishlistItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WishlistFlowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WishlistService wishlistService;

    @Test
    void addRemoveAndClearWishlist() {
        String email = "wisher-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Wisher");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(user);

        Product product = createProduct();

        List<WishlistItemResponse> afterAdd =
                wishlistService.addItem(email, new AddWishlistItemRequest(product.getId()));
        assertThat(afterAdd).hasSize(1);
        assertThat(afterAdd.get(0).productId()).isEqualTo(product.getId());

        assertThatThrownBy(() -> wishlistService.addItem(
                email, new AddWishlistItemRequest(product.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in the wishlist");

        Long wishlistItemId = afterAdd.get(0).wishlistItemId();

        List<WishlistItemResponse> afterRemove =
                wishlistService.removeItem(email, wishlistItemId);
        assertThat(afterRemove).isEmpty();

        wishlistService.addItem(email, new AddWishlistItemRequest(product.getId()));
        wishlistService.clearWishlist(email);
        assertThat(wishlistService.getWishlist(email)).isEmpty();
    }

    private Product createProduct() {
        Category category = new Category();
        category.setName("Wish Category");
        category.setSlug("wish-category-" + UUID.randomUUID());
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Wish Product");
        product.setSlug("wish-product-" + UUID.randomUUID());
        product.setSku("WP-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(5);
        product.setCategory(category);
        return productRepository.save(product);
    }
}
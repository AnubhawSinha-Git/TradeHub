package com.tradehub;

import com.tradehub.cart.CartService;
import com.tradehub.cart.dto.AddCartItemRequest;
import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.order.OrderService;
import com.tradehub.order.dto.CheckoutRequest;
import com.tradehub.order.dto.ShipToRequest;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.review.ReviewService;
import com.tradehub.review.dto.ProductReviewsResponse;
import com.tradehub.review.dto.ReviewRequest;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReviewFlowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewService reviewService;

    @Test
    void reviewRequiresPurchaseThenSupportsUpdateAndDelete() {
        String email = "reviewer-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Reviewer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(user);

        Product product = createProduct();

        assertThatThrownBy(() -> reviewService.addReview(
                email, product.getId(), new ReviewRequest(5, "Not purchased")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must purchase");

        cartService.addItem(email, new AddCartItemRequest(product.getId(), 1));
        orderService.checkout(email, new CheckoutRequest(new ShipToRequest(
                "Reviewer", "1 Main St", null, "City", "ST", "12345", "US", null), null));

        ProductReviewsResponse afterAdd = reviewService.addReview(
                email, product.getId(), new ReviewRequest(5, "Great product"));
        assertThat(afterAdd.totalReviews()).isEqualTo(1);
        assertThat(afterAdd.averageRating()).isEqualTo(5.0);

        assertThatThrownBy(() -> reviewService.addReview(
                email, product.getId(), new ReviewRequest(4, "Duplicate")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");

        ProductReviewsResponse afterUpdate = reviewService.updateReview(
                email, product.getId(), new ReviewRequest(4, "Actually nice"));
        assertThat(afterUpdate.averageRating()).isEqualTo(4.0);
        assertThat(afterUpdate.reviews().get(0).comment()).isEqualTo("Actually nice");

        Long reviewId = afterUpdate.reviews().get(0).reviewId();
        reviewService.deleteReview(email, reviewId);
        assertThat(reviewService.getProductReviews(product.getId()).totalReviews())
                .isZero();
    }

    private Product createProduct() {
        Category category = new Category();
        category.setName("Review Category");
        category.setSlug("review-category-" + UUID.randomUUID());
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Review Product");
        product.setSlug("review-product-" + UUID.randomUUID());
        product.setSku("RP-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(new BigDecimal("15.00"));
        product.setStockQuantity(10);
        product.setCategory(category);
        return productRepository.save(product);
    }
}
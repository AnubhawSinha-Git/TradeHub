package com.tradehub;

import com.tradehub.cart.CartService;
import com.tradehub.cart.dto.AddCartItemRequest;
import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.coupon.CouponService;
import com.tradehub.coupon.CouponType;
import com.tradehub.coupon.dto.CreateCouponRequest;
import com.tradehub.order.OrderService;
import com.tradehub.order.OrderStatus;
import com.tradehub.order.dto.CheckoutRequest;
import com.tradehub.order.dto.OrderResponse;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CouponFlowTest {

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
    private CouponService couponService;

    @Test
    void percentCouponAppliesDiscountAtCheckout() {
        String email = "coupon-user-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Coupon Buyer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(user);

        Product product = createProduct(new BigDecimal("50.00"), 10);

        couponService.createCoupon(new CreateCouponRequest(
                "SAVE10", CouponType.PERCENT, new BigDecimal("10.00"),
                new BigDecimal("100.00"), new BigDecimal("20.00"),
                null, LocalDateTime.now().plusDays(30), 5, true));

        cartService.addItem(email, new AddCartItemRequest(product.getId(), 4));

        OrderResponse placed = orderService.checkout(email,
                new CheckoutRequest(null, "save10"));

        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.couponCode()).isEqualTo("SAVE10");
        assertThat(placed.discountAmount()).isEqualByComparingTo("20.00");
        assertThat(placed.totalAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void expiredCouponIsRejected() {
        String email = "expired-user-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Expired Buyer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(user);

        Product product = createProduct(new BigDecimal("30.00"), 5);

        couponService.createCoupon(new CreateCouponRequest(
                "EXPIRED", CouponType.FIXED, new BigDecimal("5.00"),
                null, null,
                null, LocalDateTime.now().minusDays(1), null, true));

        cartService.addItem(email, new AddCartItemRequest(product.getId(), 1));

        assertThatThrownBy(() -> orderService.checkout(
                email, new CheckoutRequest(null, "EXPIRED")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void minOrderNotMetIsRejected() {
        String email = "min-user-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Min Buyer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(user);

        Product product = createProduct(new BigDecimal("10.00"), 5);

        couponService.createCoupon(new CreateCouponRequest(
                "MIN50", CouponType.PERCENT, new BigDecimal("10.00"),
                null, new BigDecimal("50.00"),
                null, LocalDateTime.now().plusDays(7), null, true));

        cartService.addItem(email, new AddCartItemRequest(product.getId(), 1));

        assertThatThrownBy(() -> orderService.checkout(
                email, new CheckoutRequest(null, "MIN50")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum order amount");
    }

    private Product createProduct(BigDecimal price, int stock) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = new Category();
        category.setName("Coupon Category " + suffix);
        category.setSlug("coupon-category-" + suffix);
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Coupon Product " + suffix);
        product.setSlug("coupon-product-" + suffix);
        product.setSku("CP-" + suffix);
        product.setPrice(price);
        product.setStockQuantity(stock);
        product.setCategory(category);
        return productRepository.save(product);
    }
}
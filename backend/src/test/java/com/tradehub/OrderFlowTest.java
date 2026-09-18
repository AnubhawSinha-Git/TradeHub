package com.tradehub;

import com.tradehub.cart.CartService;
import com.tradehub.cart.dto.AddCartItemRequest;
import com.tradehub.cart.dto.CartResponse;
import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.order.OrderService;
import com.tradehub.order.OrderStatus;
import com.tradehub.order.dto.CheckoutRequest;
import com.tradehub.order.dto.OrderResponse;
import com.tradehub.order.dto.ShipToRequest;
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
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderFlowTest {

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

    @Test
    void cartToCheckoutToPayToRefundFlow() {
        String email = "buyer-" + UUID.randomUUID() + "@test.com";
        User buyer = new User();
        buyer.setFullName("Buyer");
        buyer.setEmail(email);
        buyer.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        buyer.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(buyer);

        Category category = new Category();
        category.setName("Test Category");
        category.setSlug("test-category-" + UUID.randomUUID());
        categoryRepository.save(category);

        int initialStock = 10;
        Product product = new Product();
        product.setName("Test Product");
        product.setSlug("test-product-" + UUID.randomUUID());
        product.setSku("TP-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(initialStock);
        product.setCategory(category);
        productRepository.save(product);

        int quantity = 3;

        CartResponse cart = cartService.addItem(
                email,
                new AddCartItemRequest(product.getId(), quantity));
        assertThat(cart.items()).hasSize(1);
        assertThat(cart.items().get(0).quantity()).isEqualTo(quantity);

        OrderResponse placed = orderService.checkout(
                email,
                new CheckoutRequest(new ShipToRequest(
                        "Buyer",
                        "1 Main St",
                        null,
                        "Metropolis",
                        "NY",
                        "10001",
                        "US",
                        null), null));
        assertThat(placed.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(placed.totalAmount()).isEqualByComparingTo("75.00");
        assertThat(placed.items()).hasSize(1);
        assertThat(placed.shippingAddress().city()).isEqualTo("Metropolis");

        Product afterCheckout = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterCheckout.getStockQuantity()).isEqualTo(initialStock - quantity);

        OrderResponse paid = orderService.payOrder(email, placed.orderId());
        assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.payment()).isNotNull();
        assertThat(paid.payment().transactionId()).isNotBlank();
        assertThat(paid.payment().amount()).isEqualByComparingTo("75.00");

        OrderResponse refunded = orderService.refundOrder(email, placed.orderId());
        assertThat(refunded.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(refunded.payment().refunded()).isTrue();
        assertThat(refunded.payment().refundedAt()).isNotNull();

        Product afterRefund = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterRefund.getStockQuantity()).isEqualTo(initialStock);

        assertThat(orderService.getDashboard().totalOrders()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void checkoutEmptyCartIsRejected() {
        String email = "empty-" + UUID.randomUUID() + "@test.com";
        User buyer = new User();
        buyer.setFullName("Empty Buyer");
        buyer.setEmail(email);
        buyer.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        buyer.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(buyer);

        assertThatThrownBy(() -> orderService.checkout(email, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cart is empty");
    }
}
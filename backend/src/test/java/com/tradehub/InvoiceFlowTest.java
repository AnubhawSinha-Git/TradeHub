package com.tradehub;

import com.tradehub.cart.CartService;
import com.tradehub.cart.dto.AddCartItemRequest;
import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.order.InvoiceService;
import com.tradehub.order.OrderService;
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
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InvoiceFlowTest {

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
    private InvoiceService invoiceService;

    @Test
    void generatePdfInvoiceForOwnOrder() {
        String email = "invoiced-" + UUID.randomUUID() + "@test.com";
        User buyer = createUser(email);

        Product product = createProduct();

        cartService.addItem(email, new AddCartItemRequest(product.getId(), 2));
        OrderResponse order = orderService.checkout(email, new CheckoutRequest(
                new ShipToRequest("Buyer", "1 Main St", null, "Metropolis",
                        "NY", "10001", "US", null), null));

        byte[] pdf = invoiceService.generateInvoice(email, order.orderId());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1))
                .isEqualTo("%PDF-");
    }

    @Test
    void invoiceForAnotherUsersOrderIsRejected() {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@test.com";
        String otherEmail = "other-" + UUID.randomUUID() + "@test.com";
        createUser(ownerEmail);
        createUser(otherEmail);

        Product product = createProduct();
        cartService.addItem(ownerEmail, new AddCartItemRequest(product.getId(), 1));
        OrderResponse order = orderService.checkout(ownerEmail, null);

        assertThatThrownBy(() -> invoiceService.generateInvoice(otherEmail, order.orderId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User createUser(String email) {
        User user = new User();
        user.setFullName("Invoice Buyer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        return userRepository.save(user);
    }

    private Product createProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = new Category();
        category.setName("Invoice Category " + suffix);
        category.setSlug("invoice-category-" + suffix);
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Invoice Product " + suffix);
        product.setSlug("invoice-product-" + suffix);
        product.setSku("IP-" + suffix);
        product.setPrice(new BigDecimal("12.50"));
        product.setStockQuantity(10);
        product.setCategory(category);
        return productRepository.save(product);
    }
}
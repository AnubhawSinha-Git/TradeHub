package com.tradehub;

import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import com.tradehub.user.UserService;
import com.tradehub.user.dto.UpdateUserRolesRequest;
import com.tradehub.warehouse.WarehouseService;
import com.tradehub.warehouse.dto.LowStockProductResponse;
import com.tradehub.warehouse.dto.RestockItemRequest;
import com.tradehub.warehouse.dto.RestockLogResponse;
import com.tradehub.warehouse.dto.RestockRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class WarehouseFlowTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private UserService userService;

    @Test
    void restockIncreasesStockAndCreatesLog() {
        Product product = createProduct(5, 10);
        String email = createWarehouseUser();

        RestockLogResponse log = warehouseService.restock(
                product.getId(), 20, email);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(25);

        assertThat(log.productId()).isEqualTo(product.getId());
        assertThat(log.productName()).isEqualTo(product.getName());
        assertThat(log.quantity()).isEqualTo(20);
        assertThat(log.restockedBy()).isEqualTo(email);
        assertThat(log.restockedAt()).isNotNull();
    }

    @Test
    void lowStockListIncludesProductsAtOrBelowThreshold() {
        Product low = createProduct(5, 10);
        Product healthy = createProduct(50, 10);

        List<LowStockProductResponse> lowStock = warehouseService.getLowStockProducts();

        assertThat(lowStock.stream().map(LowStockProductResponse::id))
                .contains(low.getId())
                .doesNotContain(healthy.getId());

        LowStockProductResponse entry = lowStock.stream()
                .filter(p -> p.id().equals(low.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(entry.stockQuantity()).isEqualTo(5);
        assertThat(entry.lowStockThreshold()).isEqualTo(10);
        assertThat(entry.suggestedQuantity()).isEqualTo(25);
    }

    @Test
    void batchRestockRestoresMultipleProducts() {
        Product first = createProduct(3, 10);
        Product second = createProduct(2, 10);
        String email = createWarehouseUser();

        List<RestockLogResponse> logs = warehouseService.restockBatch(
                new RestockRequest(List.of(
                        new RestockItemRequest(first.getId(), 50),
                        new RestockItemRequest(second.getId(), 40))),
                email);

        assertThat(logs).hasSize(2);
        assertThat(productRepository.findById(first.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(53);
        assertThat(productRepository.findById(second.getId()).orElseThrow()
                .getStockQuantity()).isEqualTo(42);
    }

    @Test
    void restockRejectsZeroQuantity() {
        Product product = createProduct(5, 10);
        String email = createWarehouseUser();

        assertThatThrownBy(() -> warehouseService.restock(
                product.getId(), 0, email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Restock quantity");
    }

    @Test
    void restockLogsAreListedMostRecentFirst() {
        Product product = createProduct(5, 10);
        String email = createWarehouseUser();

        warehouseService.restock(product.getId(), 10, email);
        warehouseService.restock(product.getId(), 5, email);

        List<RestockLogResponse> logs = warehouseService.getRestockLogs()
                .stream()
                .filter(log -> log.productId().equals(product.getId()))
                .toList();

        assertThat(logs).hasSize(2);
        assertThat(logs.get(0).quantity()).isEqualTo(5);
    }

    @Test
    void adminCanPromoteUserToWarehouseRole() {
        String email = "promote-" + UUID.randomUUID() + "@test.com";
        User user = new User();
        user.setFullName("Staff Member");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        user.setRoles(Set.of(RoleName.CUSTOMER));
        user = userRepository.save(user);

        userService.updateRoles(user.getId(),
                new UpdateUserRolesRequest(Set.of(RoleName.WAREHOUSE, RoleName.CUSTOMER)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getRoles()).contains(RoleName.WAREHOUSE);
    }

    private Product createProduct(int stock, int threshold) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Category category = new Category();
        category.setName("Warehouse Category " + suffix);
        category.setSlug("warehouse-category-" + suffix);
        categoryRepository.save(category);

        Product product = new Product();
        product.setName("Warehouse Product " + suffix);
        product.setSlug("warehouse-product-" + suffix);
        product.setSku("WH-" + suffix);
        product.setPrice(new java.math.BigDecimal("25.00"));
        product.setStockQuantity(stock);
        product.setLowStockThreshold(threshold);
        product.setCategory(category);
        return productRepository.save(product);
    }

    private String createWarehouseUser() {
        String email = "warehouse-" + UUID.randomUUID() + "@test.com";
        User warehouse = new User();
        warehouse.setFullName("Warehouse Tester");
        warehouse.setEmail(email);
        warehouse.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        warehouse.setRoles(Set.of(RoleName.WAREHOUSE));
        userRepository.save(warehouse);
        return email;
    }
}
package com.tradehub.config;

import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.coupon.Coupon;
import com.tradehub.coupon.CouponRepository;
import com.tradehub.coupon.CouponType;
import com.tradehub.product.Product;
import com.tradehub.product.ProductRepository;
import com.tradehub.user.RoleName;
import com.tradehub.user.User;
import com.tradehub.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CouponRepository couponRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Transactional
    public CommandLineRunner seedData() {
        return args -> {
            ensureWarehouseUser();

            if (userRepository.count() > 0) {
                log.info("Database already contains data, skipping seed");
                return;
            }

            seedUsers();
            seedProducts();
            seedCoupon();
            log.info("Sample data seeded successfully");
        };
    }

    private void ensureWarehouseUser() {
        String email = "warehouse@tradehub.com";

        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }

        User warehouse = new User();
        warehouse.setFullName("Warehouse Staff");
        warehouse.setEmail(email);
        warehouse.setPasswordHash(passwordEncoder.encode("Warehouse@123"));
        warehouse.setRoles(Set.of(RoleName.WAREHOUSE));
        userRepository.save(warehouse);

        log.info("Seeded warehouse user: warehouse@tradehub.com / Warehouse@123");
    }

    private void seedUsers() {
        User admin = new User();
        admin.setFullName("Admin User");
        admin.setEmail("admin@tradehub.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setRoles(Set.of(RoleName.ADMIN));
        userRepository.save(admin);

        User customer = new User();
        customer.setFullName("Test Customer");
        customer.setEmail("customer@tradehub.com");
        customer.setPasswordHash(passwordEncoder.encode("Customer@123"));
        customer.setRoles(Set.of(RoleName.CUSTOMER));
        userRepository.save(customer);

        log.info("Seeded users: admin@tradehub.com / Admin@123, "
                + "customer@tradehub.com / Customer@123");
    }

    private void seedProducts() {
        Map<String, String[]> categoryData = new LinkedHashMap<>();
        categoryData.put("Electronics",
                new String[]{"electronics", "Gadgets, devices and accessories"});
        categoryData.put("Clothing",
                new String[]{"clothing", "Apparel for all seasons"});
        categoryData.put("Books",
                new String[]{"books", "Technical and general reading"});
        categoryData.put("Home & Kitchen",
                new String[]{"home-kitchen", "Cookware and home essentials"});

        Map<String, Category> categories = new LinkedHashMap<>();
        categoryData.forEach((name, data) -> {
            Category category = new Category();
            category.setName(name);
            category.setSlug(data[0]);
            category.setDescription(data[1]);
            categories.put(name, categoryRepository.save(category));
        });

        seedProduct(categories.get("Electronics"),
                "Wireless Headphones", "WH-001",
                "Over-ear wireless headphones with active noise cancelling",
                "129.99", 25);
        seedProduct(categories.get("Electronics"),
                "Smart Watch", "SW-001",
                "Fitness tracking smart watch with GPS",
                "199.99", 15);
        seedProduct(categories.get("Electronics"),
                "Bluetooth Speaker", "BS-001",
                "Portable waterproof bluetooth speaker",
                "59.99", 8);

        seedProduct(categories.get("Clothing"),
                "Cotton T-Shirt", "TS-001",
                "Premium 100% cotton crew neck t-shirt",
                "19.99", 100);
        seedProduct(categories.get("Clothing"),
                "Denim Jacket", "DJ-001",
                "Classic faded denim jacket",
                "79.99", 30);

        seedProduct(categories.get("Books"),
                "Spring Boot in Action", "SB-001",
                "Hands-on guide to building Spring Boot applications",
                "44.99", 50);
        seedProduct(categories.get("Books"),
                "Clean Code", "CC-001",
                "A handbook of agile software craftsmanship",
                "34.99", 60);

        seedProduct(categories.get("Home & Kitchen"),
                "Cookware Set", "CW-001",
                "10-piece stainless steel cookware set",
                "149.99", 20);
        seedProduct(categories.get("Home & Kitchen"),
                "Coffee Maker", "CM-001",
                "Programmable drip coffee maker with carafe",
                "89.99", 35);
    }

    private void seedCoupon() {
        Coupon coupon = new Coupon();
        coupon.setCode("WELCOME10");
        coupon.setType(CouponType.PERCENT);
        coupon.setValue(new BigDecimal("10.00"));
        coupon.setMinOrderAmount(new BigDecimal("20.00"));
        coupon.setValidUntil(LocalDateTime.now().plusDays(90));
        coupon.setMaxUses(1000);
        couponRepository.save(coupon);

        log.info("Seeded coupon: WELCOME10 (10% off, min order 20.00)");
    }

    private void seedProduct(
            Category category,
            String name,
            String sku,
            String description,
            String price,
            int stockQuantity) {
        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setSlug(createSlug(name + "-" + sku));
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        productRepository.save(product);
    }

    private String createSlug(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
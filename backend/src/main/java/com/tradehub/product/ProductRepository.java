package com.tradehub.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p "
            + "where p.stockQuantity <= p.lowStockThreshold "
            + "order by p.stockQuantity asc")
    List<Product> findLowStock();

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySkuIgnoreCase(String sku);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndCategorySlug(
            String categorySlug,
            Pageable pageable);

    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(
            String keyword,
            Pageable pageable);

    Page<Product> findByActiveTrueAndCategorySlugAndNameContainingIgnoreCase(
            String categorySlug,
            String keyword,
            Pageable pageable);
}
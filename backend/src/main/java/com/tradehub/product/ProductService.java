package com.tradehub.product;

import com.tradehub.category.Category;
import com.tradehub.category.CategoryRepository;
import com.tradehub.common.dto.PageResponse;
import com.tradehub.common.exception.ResourceNotFoundException;
import com.tradehub.product.dto.CreateProductRequest;
import com.tradehub.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tradehub.product.dto.UpdateProductRequest;

import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        String normalizedSku = request.sku().trim().toUpperCase(Locale.ROOT);

        if (productRepository.existsBySkuIgnoreCase(normalizedSku)) {
            throw new IllegalArgumentException("SKU already exists");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.isActive()) {
            throw new IllegalArgumentException("Category is inactive");
        }

        String slug = createSlug(
                request.name().trim() + "-" + normalizedSku);

        if (productRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Product slug already exists");
        }

        Product product = new Product();
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setSku(normalizedSku);
        product.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setLowStockThreshold(
                request.lowStockThreshold() == null ? 10 : request.lowStockThreshold());
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (request.active() && !category.isActive()) {
            throw new IllegalArgumentException(
                    "An active product cannot belong to an inactive category");
        }

        product.setName(request.name().trim());
        product.setDescription(
                request.description() == null ? null : request.description().trim());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setLowStockThreshold(
                request.lowStockThreshold() == null ? 10 : request.lowStockThreshold());
        product.setCategory(category);
        product.setActive(request.active());

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String categorySlug,
            String keyword,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending());

        boolean hasCategory = categorySlug != null && !categorySlug.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Product> productPage;

        if (hasCategory && hasKeyword) {
            productPage = productRepository
                    .findByActiveTrueAndCategorySlugAndNameContainingIgnoreCase(
                            categorySlug.trim().toLowerCase(Locale.ROOT),
                            keyword.trim(),
                            pageable);
        } else if (hasCategory) {
            productPage = productRepository.findByActiveTrueAndCategorySlug(
                    categorySlug.trim().toLowerCase(Locale.ROOT),
                    pageable);
        } else if (hasKeyword) {
            productPage = productRepository.findByActiveTrueAndNameContainingIgnoreCase(
                    keyword.trim(),
                    pageable);
        } else {
            productPage = productRepository.findByActiveTrue(pageable);
        }

        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isFirst(),
                productPage.isLast());
    }

    private String createSlug(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.isActive(),
                category.getId(),
                category.getName(),
                category.getSlug(),
                product.getCreatedAt());
    }
}
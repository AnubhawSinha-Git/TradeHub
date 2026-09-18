package com.tradehub.category;

import com.tradehub.category.dto.CategoryResponse;
import com.tradehub.category.dto.CreateCategoryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedName = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Category name already exists");
        }

        String slug = createSlug(normalizedName);

        if (slug.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name must contain letters or numbers"
            );
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Category slug already exists");
        }

        Category category = new Category();
        category.setName(normalizedName);
        category.setSlug(slug);
        category.setDescription(
                request.description() == null ? null : request.description().trim()
        );

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String createSlug(String value) {
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt()
        );
    }
}
package com.enterprise.ticket.service;

import com.enterprise.ticket.model.Category;
import com.enterprise.ticket.model.Subcategory;
import com.enterprise.ticket.repository.CategoryRepository;
import com.enterprise.ticket.repository.SubcategoryRepository;
import com.enterprise.ticket.request.CreateCategoryRequest;
import com.enterprise.ticket.request.CreateSubcategoryRequest;
import com.enterprise.ticket.response.CategoryResponse;
import com.enterprise.ticket.response.SubcategoryResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing configurable categories and subcategories (FR-005).
 * Uses explicit read-only transactions for queries and write transactions for mutations.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final SubcategoryRepository subcategoryRepository;

  @Transactional(readOnly = true)
  public List<CategoryResponse> getAllActiveCategories() {
    return categoryRepository.findByIsActiveTrue().stream()
        .map(this::toCategoryResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategoryById(Long categoryId) {
    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
    return toCategoryResponse(category);
  }

  @Transactional
  public CategoryResponse createCategory(CreateCategoryRequest request) {
    validateCategoryRequest(request);

    if (categoryRepository.existsByCode(request.getCode().trim().toUpperCase())) {
      throw new IllegalArgumentException("Category code already exists: " + request.getCode());
    }
    if (categoryRepository.existsByName(request.getName().trim())) {
      throw new IllegalArgumentException("Category name already exists: " + request.getName());
    }

    Category category =
        Category.builder()
            .name(request.getName().trim())
            .code(request.getCode().trim().toUpperCase())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .isActive(true)
            .build();

    category = categoryRepository.save(category);
    return toCategoryResponse(category);
  }

  @Transactional
  public SubcategoryResponse createSubcategory(Long categoryId, CreateSubcategoryRequest request) {
    validateSubcategoryRequest(request);

    Category category =
        categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

    if (subcategoryRepository.existsByCategoryIdAndCode(categoryId, request.getCode().trim().toUpperCase())) {
      throw new IllegalArgumentException("Subcategory code already exists under this category: " + request.getCode());
    }

    Subcategory subcategory =
        Subcategory.builder()
            .category(category)
            .name(request.getName().trim())
            .code(request.getCode().trim().toUpperCase())
            .description(request.getDescription() != null ? request.getDescription().trim() : null)
            .isActive(true)
            .build();

    subcategory = subcategoryRepository.save(subcategory);
    return toSubcategoryResponse(subcategory);
  }

  private void validateCategoryRequest(CreateCategoryRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Category payload cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("Category name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw new IllegalArgumentException("Category code is required");
    }
  }

  private void validateSubcategoryRequest(CreateSubcategoryRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Subcategory payload cannot be empty");
    }
    if (request.getName() == null || request.getName().trim().isEmpty()) {
      throw new IllegalArgumentException("Subcategory name is required");
    }
    if (request.getCode() == null || request.getCode().trim().isEmpty()) {
      throw new IllegalArgumentException("Subcategory code is required");
    }
  }

  private CategoryResponse toCategoryResponse(Category category) {
    List<SubcategoryResponse> subResp =
        category.getSubcategories().stream()
            .filter(Subcategory::getIsActive)
            .map(this::toSubcategoryResponse)
            .collect(Collectors.toList());

    return CategoryResponse.builder()
        .id(category.getId())
        .name(category.getName())
        .code(category.getCode())
        .description(category.getDescription())
        .isActive(category.getIsActive())
        .subcategories(subResp)
        .createdAt(category.getCreatedAt())
        .build();
  }

  private SubcategoryResponse toSubcategoryResponse(Subcategory subcategory) {
    return SubcategoryResponse.builder()
        .id(subcategory.getId())
        .categoryId(subcategory.getCategory().getId())
        .name(subcategory.getName())
        .code(subcategory.getCode())
        .description(subcategory.getDescription())
        .isActive(subcategory.getIsActive())
        .createdAt(subcategory.getCreatedAt())
        .build();
  }
}

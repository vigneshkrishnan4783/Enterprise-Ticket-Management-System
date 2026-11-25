package com.enterprise.ticket.controller;

import com.enterprise.ticket.request.CreateCategoryRequest;
import com.enterprise.ticket.request.CreateSubcategoryRequest;
import com.enterprise.ticket.response.ApiResponse;
import com.enterprise.ticket.response.CategoryResponse;
import com.enterprise.ticket.response.ResponseBuilder;
import com.enterprise.ticket.response.SubcategoryResponse;
import com.enterprise.ticket.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for managing configurable categories and subcategories")
public class CategoryController {

  private final CategoryService categoryService;

  @GetMapping
  @Operation(summary = "List categories", description = "Returns all active categories with their nested subcategories")
  public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategories() {
    return ResponseBuilder.<List<CategoryResponse>>builder()
        .success()
        .withData(categoryService.getAllActiveCategories())
        .build();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get category by ID", description = "Retrieves category details by category ID")
  public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
    return ResponseBuilder.<CategoryResponse>builder()
        .success()
        .withData(categoryService.getCategoryById(id))
        .build();
  }

  @PostMapping
  @Operation(summary = "Create category", description = "Creates a new root ticket category")
  public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
      @RequestBody CreateCategoryRequest request) {
    return ResponseBuilder.<CategoryResponse>builder()
        .created("Category created successfully")
        .withData(categoryService.createCategory(request))
        .build();
  }

  @PostMapping("/{id}/subcategories")
  @Operation(summary = "Create subcategory", description = "Creates a subcategory under a specified category")
  public ResponseEntity<ApiResponse<SubcategoryResponse>> createSubcategory(
      @PathVariable Long id, @RequestBody CreateSubcategoryRequest request) {
    return ResponseBuilder.<SubcategoryResponse>builder()
        .created("Subcategory created successfully")
        .withData(categoryService.createSubcategory(id, request))
        .build();
  }
}

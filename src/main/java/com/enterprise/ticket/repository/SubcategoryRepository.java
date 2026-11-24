package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Subcategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

  List<Subcategory> findByCategoryIdAndIsActiveTrue(Long categoryId);

  Optional<Subcategory> findByIdAndCategoryId(Long id, Long categoryId);

  boolean existsByCategoryIdAndCode(Long categoryId, String code);
}

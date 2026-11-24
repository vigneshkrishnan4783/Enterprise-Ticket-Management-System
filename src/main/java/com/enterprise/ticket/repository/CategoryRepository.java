package com.enterprise.ticket.repository;

import com.enterprise.ticket.model.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  List<Category> findByIsActiveTrue();

  Optional<Category> findByCode(String code);

  boolean existsByCode(String code);

  boolean existsByName(String name);
}

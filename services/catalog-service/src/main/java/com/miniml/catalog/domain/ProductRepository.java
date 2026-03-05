package com.miniml.catalog.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

        Page<Product> findByActiveTrue(Pageable pageable);

        Page<Product> findByActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

        @Query("""
                        SELECT p FROM Product p
                        WHERE p.active = true
                          AND (:query = '' OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')))
                          AND (:category = '' OR LOWER(p.category) = LOWER(:category))
                        """)
        Page<Product> search(
                        @Param("query") String query,
                        @Param("category") String category,
                        Pageable pageable);

        Optional<Product> findByIdAndActiveTrue(UUID id);
}

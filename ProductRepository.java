package com.noble.shop.repository;

import com.noble.shop.model.Product;
import com.noble.shop.model.Product.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByPublishedTrue(Pageable pageable);
    Page<Product> findByCategoryAndPublishedTrue(Category category, Pageable pageable);
    List<Product> findByCategoryAndPublishedTrueOrderByCreatedAtDesc(Category category);
    List<Product> findByPublishedTrueOrderByCreatedAtDesc();
    List<Product> findByPublishedTrueOrderBySalesCountDesc();
    List<Product> findTop4ByCategoryAndPublishedTrueAndIdNotOrderBySalesCountDesc(Category category, Long id);
    @Query("SELECT p FROM Product p WHERE p.published = true AND (p.nameEn LIKE %:q% OR p.model LIKE %:q%)")
    List<Product> search(@Param("q") String q);
    @Query("SELECT p FROM Product p WHERE p.published = true AND (p.nameEn LIKE %:q% OR p.model LIKE %:q%)")
    Page<Product> search(@Param("q") String q, Pageable pageable);
    @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    java.util.Optional<Product> findByIdWithImages(@Param("id") Long id);
}

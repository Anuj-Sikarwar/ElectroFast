package com.quickdeliver.repository;

import com.quickdeliver.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.city WHERE s.city.id = :cityId AND s.status = 'ACTIVE' AND p.available = true")
    Page<Product> findByCityId(@Param("cityId") Long cityId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.city WHERE s.city.id = :cityId AND s.status = 'ACTIVE' AND p.available = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.model) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> searchInCity(@Param("cityId") Long cityId, @Param("q") String query, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.city WHERE s.city.id = :cityId AND p.category.id = :categoryId AND s.status = 'ACTIVE' AND p.available = true")
    Page<Product> findByCityIdAndCategoryId(@Param("cityId") Long cityId, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.shop s JOIN FETCH s.city WHERE s.id = :shopId AND p.available = true")
    Page<Product> findByShopIdAndAvailableTrue(@Param("shopId") Long shopId, Pageable pageable);
}
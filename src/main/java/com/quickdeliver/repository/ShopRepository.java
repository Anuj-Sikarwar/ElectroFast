package com.quickdeliver.repository;

import com.quickdeliver.entity.Shop;
import com.quickdeliver.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query("SELECT s FROM Shop s JOIN FETCH s.city WHERE s.city.id = :cityId AND s.status = :status")
    Page<Shop> findByCityIdAndStatus(@Param("cityId") Long cityId, @Param("status") ShopStatus status, Pageable pageable);

    @Query("SELECT s FROM Shop s JOIN FETCH s.city LEFT JOIN FETCH s.registeredBy")
    Page<Shop> findAllWithCity(Pageable pageable);

    Page<Shop> findByRegisteredById(Long memberId, Pageable pageable);
}
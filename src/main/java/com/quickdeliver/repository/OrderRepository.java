package com.quickdeliver.repository;

import com.quickdeliver.entity.Order;
import com.quickdeliver.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    // Delivery guy — orders assigned to him
    Page<Order> findByAssignedToIdOrderByCreatedAtDesc(Long deliveryUserId, Pageable pageable);

    // Delivery guy — assigned orders filtered by status
    Page<Order> findByAssignedToIdAndStatusOrderByCreatedAtDesc(Long deliveryUserId, OrderStatus status, Pageable pageable);

    // Unassigned confirmed orders (available for delivery guys to pick up)
    @Query("SELECT o FROM Order o WHERE o.assignedTo IS NULL AND o.status = 'CONFIRMED' ORDER BY o.createdAt ASC")
    List<Order> findUnassignedConfirmedOrders();

    @Query("SELECT o FROM Order o JOIN o.user u WHERE u.city.id = :cityId AND o.status = :status ORDER BY o.createdAt DESC")
    Page<Order> findByStatusAndCityId(@Param("status") OrderStatus status, @Param("cityId") Long cityId, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.user u WHERE u.city.id = :cityId ORDER BY o.createdAt DESC")
    Page<Order> findByCityId(@Param("cityId") Long cityId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :from AND o.createdAt < :to ORDER BY o.createdAt DESC")
    Page<Order> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :from AND o.createdAt < :to ORDER BY o.createdAt DESC")
    Page<Order> findByStatusAndDateRange(@Param("status") OrderStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.user u WHERE u.city.id = :cityId AND o.createdAt >= :from AND o.createdAt < :to ORDER BY o.createdAt DESC")
    Page<Order> findByCityIdAndDateRange(@Param("cityId") Long cityId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT o FROM Order o JOIN o.user u WHERE o.status = :status AND u.city.id = :cityId AND o.createdAt >= :from AND o.createdAt < :to ORDER BY o.createdAt DESC")
    Page<Order> findByStatusAndCityIdAndDateRange(@Param("status") OrderStatus status, @Param("cityId") Long cityId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.user u WHERE u.city.id = :cityId")
    long countByCityId(@Param("cityId") Long cityId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o JOIN o.user u WHERE u.city.id = :cityId AND o.status = 'DELIVERED'")
    BigDecimal totalRevenueByCityId(@Param("cityId") Long cityId);
}

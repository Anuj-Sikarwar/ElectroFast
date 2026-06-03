package com.quickdeliver.service;

import com.quickdeliver.dto.response.OrderResponse;
import com.quickdeliver.entity.Order;
import com.quickdeliver.entity.Payment;
import com.quickdeliver.entity.User;
import com.quickdeliver.enums.OrderStatus;
import com.quickdeliver.enums.PaymentMethod;
import com.quickdeliver.enums.PaymentStatus;
import com.quickdeliver.enums.Role;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.OrderRepository;
import com.quickdeliver.repository.PaymentRepository;
import com.quickdeliver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;
    private final PaymentRepository paymentRepository;

    // ── Get all unassigned confirmed orders (delivery guy picks one) ──────────
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders() {
        return orderRepository.findUnassignedConfirmedOrders()
                .stream().map(OrderResponse::from).toList();
    }

    // ── Delivery guy accepts an order — assigns himself ───────────────────────
    @Transactional
    public OrderResponse acceptOrder(String deliveryEmail, Long orderId) {
        User delivery = getDeliveryUser(deliveryEmail);
        Order order   = getOrderOrThrow(orderId);

        if (order.getAssignedTo() != null)
            throw new BadRequestException("Order already assigned to another delivery person");

        if (order.getStatus() != OrderStatus.CONFIRMED)
            throw new BadRequestException("Order is not in CONFIRMED state");

        order.setAssignedTo(delivery);
        log.info("Order {} accepted by delivery user {}", orderId, deliveryEmail);
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Get orders assigned to this delivery guy ──────────────────────────────
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyDeliveries(String deliveryEmail, Pageable pageable) {
        User delivery = getDeliveryUser(deliveryEmail);
        return orderRepository
                .findByAssignedToIdOrderByCreatedAtDesc(delivery.getId(), pageable)
                .map(OrderResponse::from);
    }

    // ── Mark order as out for delivery ────────────────────────────────────────
    @Transactional
    public OrderResponse markOutForDelivery(String deliveryEmail, Long orderId) {
        Order order = getAssignedOrder(deliveryEmail, orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED)
            throw new BadRequestException("Order must be CONFIRMED before marking out for delivery");

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        log.info("Order {} marked OUT_FOR_DELIVERY by {}", orderId, deliveryEmail);
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Mark order as delivered + collect COD if applicable ───────────────────
    @Transactional
    public OrderResponse markDelivered(String deliveryEmail, Long orderId) {
        Order order = getAssignedOrder(deliveryEmail, orderId);

        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY)
            throw new BadRequestException("Order must be OUT_FOR_DELIVERY before marking as delivered");

        order.setStatus(OrderStatus.DELIVERED);

        // If COD — mark payment collected
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            paymentRepository.findByOrderId(orderId).ifPresent(p -> {
                p.setStatus(PaymentStatus.SUCCESS);
                p.setPaidAt(LocalDateTime.now());
                paymentRepository.save(p);
            });
        }

        log.info("Order {} DELIVERED by {}", orderId, deliveryEmail);
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Admin: assign a specific delivery guy to an order ────────────────────
    @Transactional
    public OrderResponse assignDelivery(Long orderId, Long deliveryUserId) {
        Order order   = getOrderOrThrow(orderId);
        User  delivery = userRepository.findById(deliveryUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery user not found"));

        if (delivery.getRole() != Role.DELIVERY)
            throw new BadRequestException("User is not a delivery person");

        order.setAssignedTo(delivery);
        log.info("Order {} assigned to delivery user {}", orderId, deliveryUserId);
        return OrderResponse.from(orderRepository.save(order));
    }

    // ── Admin: get all delivery personnel ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<User> getAllDeliveryPersonnel() {
        return userRepository.findByRole(Role.DELIVERY);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private User getDeliveryUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.DELIVERY)
            throw new BadRequestException("Access denied — not a delivery account");
        return user;
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private Order getAssignedOrder(String deliveryEmail, Long orderId) {
        User  delivery = getDeliveryUser(deliveryEmail);
        Order order    = getOrderOrThrow(orderId);

        if (order.getAssignedTo() == null || !order.getAssignedTo().getId().equals(delivery.getId()))
            throw new BadRequestException("This order is not assigned to you");

        return order;
    }
}

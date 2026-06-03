package com.quickdeliver.service;

import com.quickdeliver.dto.request.OrderRequest;
import com.quickdeliver.dto.response.OrderResponse;
import com.quickdeliver.entity.*;
import com.quickdeliver.enums.OrderStatus;
import com.quickdeliver.enums.PaymentMethod;
import com.quickdeliver.enums.PaymentStatus;
import com.quickdeliver.exception.BadRequestException;
import com.quickdeliver.exception.ResourceNotFoundException;
import com.quickdeliver.repository.OrderRepository;
import com.quickdeliver.repository.PaymentRepository;
import com.quickdeliver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository   orderRepository;
    private final UserRepository    userRepository;
    private final ProductService    productService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse placeOrder(String userEmail, OrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setUser(user);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryPhone(request.getDeliveryPhone());
        order.setNotes(request.getNotes());
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productService.getProductOrThrow(itemReq.getProductId());

            if (!product.isAvailable())
                throw new BadRequestException("Product not available: " + product.getName());
            if (product.getStockQuantity() < itemReq.getQuantity())
                throw new BadRequestException("Insufficient stock for: " + product.getName());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            if (product.getStockQuantity() == 0) product.setAvailable(false);

            items.add(item);
            total = total.add(item.getSubtotal());
        }

        order.setItems(items);
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        notificationService.sendOrderNotification(saved);

        if (request.getPaymentMethod() == PaymentMethod.COD) {
            Payment payment = new Payment();
            payment.setOrder(saved);
            payment.setMethod(PaymentMethod.COD);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setAmount(total);
            paymentRepository.save(payment);
            saved.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(saved);

        }


        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId) {
        return OrderResponse.from(orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId)));
    }

    public void updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    /**
     * Admin: get all orders with optional filters — status, cityId, date
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersFiltered(
            OrderStatus status, Long cityId, LocalDate date, Pageable pageable) {

        boolean hasStatus = status != null;
        boolean hasCity   = cityId != null;
        boolean hasDate   = date != null;

        LocalDateTime from = hasDate ? date.atStartOfDay()           : null;
        LocalDateTime to   = hasDate ? date.plusDays(1).atStartOfDay() : null;

        Page<Order> orders;

        if (hasStatus && hasCity && hasDate) {
            orders = orderRepository.findByStatusAndCityIdAndDateRange(status, cityId, from, to, pageable);
        } else if (hasStatus && hasCity) {
            orders = orderRepository.findByStatusAndCityId(status, cityId, pageable);
        } else if (hasStatus && hasDate) {
            orders = orderRepository.findByStatusAndDateRange(status, from, to, pageable);
        } else if (hasCity && hasDate) {
            orders = orderRepository.findByCityIdAndDateRange(cityId, from, to, pageable);
        } else if (hasStatus) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (hasCity) {
            orders = orderRepository.findByCityId(cityId, pageable);
        } else if (hasDate) {
            orders = orderRepository.findByDateRange(from, to, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }

        return orders.map(OrderResponse::from);
    }
}

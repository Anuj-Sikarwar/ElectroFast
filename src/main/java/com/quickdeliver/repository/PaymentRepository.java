package com.quickdeliver.repository;

import com.quickdeliver.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByMerchantTransactionId(String merchantTransactionId);
    Optional<Payment> findByOrderId(Long orderId);
}

package com.quickdeliver.entity;

import com.quickdeliver.enums.PaymentMethod;
import com.quickdeliver.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // PhonePe fields (null for COD)
    @Column(unique = true)
    private String merchantTransactionId;   // our unique tx ID sent to PhonePe

    private String phonepeTransactionId;    // PhonePe's transaction ID (from callback)
    private String paymentInstrumentType;   // UPI / CARD / NET_BANKING etc.

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}

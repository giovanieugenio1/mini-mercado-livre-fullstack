package com.miniml.payment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
    Page<Payment> findByCustomerIdAndStatus(UUID customerId, PaymentStatus status, Pageable pageable);
}

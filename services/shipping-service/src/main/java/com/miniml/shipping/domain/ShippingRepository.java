package com.miniml.shipping.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ShippingRepository extends JpaRepository<Shipping, UUID> {

    Optional<Shipping> findByOrderId(UUID orderId);

    @Query("SELECT COUNT(s) FROM Shipping s WHERE s.status = :status")
    long countByStatus(String status);
}

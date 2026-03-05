package com.miniml.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.items WHERE r.orderId = :orderId")
    Optional<Reservation> findByOrderIdWithItems(UUID orderId);
}

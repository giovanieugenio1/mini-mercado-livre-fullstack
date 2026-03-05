package com.miniml.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductStock p WHERE p.productId = :productId")
    Optional<ProductStock> findByIdForUpdate(UUID productId);
}

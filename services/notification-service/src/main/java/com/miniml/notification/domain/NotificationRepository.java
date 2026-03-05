package com.miniml.notification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByOrderId(UUID orderId, Pageable pageable);

    Page<Notification> findByCustomerId(UUID customerId, Pageable pageable);
}

package com.miniml.notification.controller;

import com.miniml.notification.dto.NotificationResponse;
import com.miniml.notification.dto.PageResponse;
import com.miniml.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notificações", description = "Consulta de notificações — todos os endpoints exigem autenticação JWT")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Lista todas as notificações (mais recentes primeiro)",
               security = @SecurityRequirement(name = "Bearer"))
    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.listAll(page, size));
    }

    @Operation(summary = "Lista notificações de um pedido específico",
               security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PageResponse<NotificationResponse>> listByOrder(
            @PathVariable UUID orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.listByOrderId(orderId, page, size));
    }

    @Operation(summary = "Lista notificações de um cliente específico",
               security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<PageResponse<NotificationResponse>> listByCustomer(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.listByCustomerId(customerId, page, size));
    }
}

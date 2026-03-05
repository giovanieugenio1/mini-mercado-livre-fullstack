package com.miniml.payment.controller;

import com.miniml.payment.domain.PaymentStatus;
import com.miniml.payment.dto.PageResponse;
import com.miniml.payment.dto.PaymentResponse;
import com.miniml.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Pagamentos", description = "Consulta de pagamentos — todos os endpoints exigem autenticação JWT")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Busca pagamento por ID", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.findById(id));
    }

    @Operation(summary = "Busca pagamento pelo ID do pedido", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado para o pedido")
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> findByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.findByOrderId(orderId));
    }

    @Operation(summary = "Lista pagamentos com paginação e filtros opcionais",
               security = @SecurityRequirement(name = "Bearer"))
    @GetMapping
    public ResponseEntity<PageResponse<PaymentResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(paymentService.list(customerId, status, page, size));
    }
}

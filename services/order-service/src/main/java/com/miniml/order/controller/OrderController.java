package com.miniml.order.controller;

import com.miniml.order.domain.OrderStatus;
import com.miniml.order.dto.CreateOrderRequest;
import com.miniml.order.dto.OrderResponse;
import com.miniml.order.dto.PageResponse;
import com.miniml.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/orders")
@Tag(name = "Pedidos", description = "Gestão de pedidos — criação exige ROLE_USER, leitura é pública")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Cria pedido", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "201", description = "Pedido criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(order.id()).toUri();
        return ResponseEntity.created(location).body(order);
    }

    @Operation(summary = "Busca pedido por ID")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @Operation(summary = "Cancela pedido",
               description = "Cancela pedido em status CREATED. Retorna 409 se já em processamento.",
               security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "409", description = "Pedido não pode ser cancelado no status atual")
    @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }

    @Operation(summary = "Lista pedidos com paginação e filtros opcionais")
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(orderService.list(customerId, status, page, size));
    }
}

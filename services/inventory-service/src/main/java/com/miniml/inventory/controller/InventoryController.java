package com.miniml.inventory.controller;

import com.miniml.inventory.dto.AddStockRequest;
import com.miniml.inventory.dto.PageResponse;
import com.miniml.inventory.dto.ProductStockResponse;
import com.miniml.inventory.dto.ReservationResponse;
import com.miniml.inventory.dto.UpdateStockRequest;
import com.miniml.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Estoque", description = "Gestão de estoque — leitura exige autenticação, escrita exige ROLE_ADMIN")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Busca estoque de um produto", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado para o produto")
    @GetMapping("/inventory/{productId}")
    public ResponseEntity<ProductStockResponse> getStock(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.findStock(productId));
    }

    @Operation(summary = "Lista estoques com paginação", security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/inventory")
    public ResponseEntity<PageResponse<ProductStockResponse>> listStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(inventoryService.listStock(page, size));
    }

    @Operation(summary = "Busca reserva de estoque pelo ID do pedido", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Reserva não encontrada para o pedido")
    @GetMapping("/reservations/order/{orderId}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID orderId) {
        return ResponseEntity.ok(inventoryService.findReservation(orderId));
    }

    @Operation(summary = "Cadastra estoque inicial de um produto", description = "Retorna 409 se já existir estoque para o produto.", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "409", description = "Estoque já cadastrado para o produto")
    @PostMapping("/inventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductStockResponse> addStock(@Valid @RequestBody AddStockRequest request) {
        return ResponseEntity.ok(inventoryService.addStock(request.productId(), request.availableQty()));
    }

    @Operation(summary = "Redefine quantidade disponível em estoque", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Estoque não encontrado para o produto")
    @PutMapping("/inventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductStockResponse> updateStock(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return ResponseEntity.ok(inventoryService.updateStock(productId, request.availableQty()));
    }
}

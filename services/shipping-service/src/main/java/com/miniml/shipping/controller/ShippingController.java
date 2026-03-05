package com.miniml.shipping.controller;

import com.miniml.shipping.dto.PageResponse;
import com.miniml.shipping.dto.ShippingResponse;
import com.miniml.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Envios", description = "Consulta de envios — todos os endpoints exigem autenticação JWT")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @Operation(summary = "Busca envio pelo ID do pedido", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Envio não encontrado para o pedido")
    @GetMapping("/shippings/order/{orderId}")
    public ResponseEntity<ShippingResponse> getByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(shippingService.findByOrderId(orderId));
    }

    @Operation(summary = "Lista todos os envios com paginação",
               security = @SecurityRequirement(name = "Bearer"))
    @GetMapping("/shippings")
    public ResponseEntity<PageResponse<ShippingResponse>> listShippings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shippingService.listShippings(page, size));
    }
}

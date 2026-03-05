package com.miniml.catalog.controller;

import com.miniml.catalog.dto.CreateProductRequest;
import com.miniml.catalog.dto.PageResponse;
import com.miniml.catalog.dto.ProductResponse;
import com.miniml.catalog.dto.UpdateProductRequest;
import com.miniml.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Produtos", description = "Catálogo de produtos — leitura pública, escrita requer ROLE_ADMIN")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Lista produtos com paginação e filtros opcionais", description = "Filtra por texto livre (query) e/ou categoria. Acesso público.")
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.search(query, category, page, size));
    }

    @Operation(summary = "Busca produto por ID")
    @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @Operation(summary = "Cria produto", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "201", description = "Produto criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        var product = productService.createProduct(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(product.id()).toUri();
        return ResponseEntity.created(location).body(product);
    }

    @Operation(summary = "Atualiza produto completo", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @Operation(summary = "Remove produto (soft delete)", security = @SecurityRequirement(name = "Bearer"))
    @ApiResponse(responseCode = "204", description = "Produto removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

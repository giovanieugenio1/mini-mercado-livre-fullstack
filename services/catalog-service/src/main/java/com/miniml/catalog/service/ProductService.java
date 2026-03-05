package com.miniml.catalog.service;

import com.miniml.catalog.domain.Product;
import com.miniml.catalog.domain.ProductRepository;
import com.miniml.catalog.dto.CreateProductRequest;
import com.miniml.catalog.dto.PageResponse;
import com.miniml.catalog.dto.ProductResponse;
import com.miniml.catalog.dto.UpdateProductRequest;
import com.miniml.catalog.exception.ProductNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductResponse> search(String query, String category, int page, int size) {
        int safeSize = Math.min(size, MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, safeSize, Sort.by("title").ascending());

        boolean hasFilter = (query != null && !query.isBlank()) || (category != null && !category.isBlank());

        var result = hasFilter
                ? productRepository.search(
                        (query != null && !query.isBlank()) ? query.trim() : "",
                        (category != null && !category.isBlank()) ? category.trim() : "",
                        pageable)
                : productRepository.findByActiveTrue(pageable);

        log.debug("search(query={}, category={}) → {} resultados", query, category, result.getTotalElements());
        return PageResponse.from(result, ProductResponse::from);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        log.debug("findById({}) → {}", id, product.getTitle());
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        var product = Product.create(
                request.title(), request.description(), request.price(),
                request.stock(), request.category(), request.imageUrl());
        productRepository.save(product);
        log.info("Produto criado: id={} title={}", product.getId(), product.getTitle());
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        var product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.update(request.title(), request.description(), request.price(),
                request.stock(), request.category(), request.imageUrl());
        productRepository.save(product);
        log.info("Produto atualizado: id={}", id);
        return ProductResponse.from(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(UUID id) {
        var product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.deactivate();
        productRepository.save(product);
        log.info("Produto desativado (soft delete): id={}", id);
    }
}

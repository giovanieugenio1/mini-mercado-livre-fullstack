package com.miniml.catalog.service;

import com.miniml.catalog.domain.Product;
import com.miniml.catalog.domain.ProductRepository;
import com.miniml.catalog.dto.CreateProductRequest;
import com.miniml.catalog.dto.UpdateProductRequest;
import com.miniml.catalog.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @InjectMocks
    ProductService productService;

    private Product iphone;

    @BeforeEach
    void setUp() throws Exception {
        iphone = buildProduct("iPhone 15 Pro", "ELECTRONICS", new BigDecimal("7999.99"), 50);
    }

    @Test
    void search_semFiltros_retornaPaginaDeAtivos() {
        when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(iphone)));

        var result = productService.search(null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).title()).isEqualTo("iPhone 15 Pro");
        assertThat(result.totalElements()).isEqualTo(1);
        verify(productRepository).findByActiveTrue(any(Pageable.class));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    void search_comFiltros_chamaMetodoSearch() {
        when(productRepository.search(eq("iphone"), eq("ELECTRONICS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(iphone)));

        var result = productService.search("iphone", "ELECTRONICS", 0, 10);

        assertThat(result.content()).hasSize(1);
        verify(productRepository).search(eq("iphone"), eq("ELECTRONICS"), any(Pageable.class));
    }

    @Test
    void search_paginacaoLimitadaA100() {
        when(productRepository.findByActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        productService.search(null, null, 0, 9999);

        verify(productRepository).findByActiveTrue(argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void findById_produtoExistente_retornaDto() {
        when(productRepository.findByIdAndActiveTrue(iphone.getId()))
                .thenReturn(Optional.of(iphone));

        var result = productService.findById(iphone.getId());

        assertThat(result.id()).isEqualTo(iphone.getId());
        assertThat(result.price()).isEqualByComparingTo("7999.99");
    }

    @Test
    void findById_produtoNaoEncontrado_lancaNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndActiveTrue(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── createProduct ─────────────────────────────────────────

    @Test
    void createProduct_salvaERetornaDto() {
        var request = new CreateProductRequest("Galaxy S24", "Top", new BigDecimal("4999"), 10, "ELECTRONICS", null);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = productService.createProduct(request);

        assertThat(result.title()).isEqualTo("Galaxy S24");
        assertThat(result.price()).isEqualByComparingTo("4999");
        assertThat(result.active()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    // ── updateProduct ─────────────────────────────────────────

    @Test
    void updateProduct_produtoExistente_atualizaERetorna() {
        var id = iphone.getId();
        var request = new UpdateProductRequest("iPhone 15 Pro Max", "Novo", new BigDecimal("9999"), 30, "ELECTRONICS", null);
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(iphone));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = productService.updateProduct(id, request);

        assertThat(result.title()).isEqualTo("iPhone 15 Pro Max");
        assertThat(result.price()).isEqualByComparingTo("9999");
        assertThat(result.stock()).isEqualTo(30);
    }

    @Test
    void updateProduct_produtoNaoEncontrado_lancaException() {
        var id = UUID.randomUUID();
        var request = new UpdateProductRequest("X", null, BigDecimal.ONE, 0, "CAT", null);
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(id, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ── deleteProduct ─────────────────────────────────────────

    @Test
    void deleteProduct_desativaEPersiste() {
        var id = iphone.getId();
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(iphone));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.deleteProduct(id);

        var captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void deleteProduct_produtoNaoEncontrado_lancaException() {
        var id = UUID.randomUUID();
        when(productRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(id))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ── helper ───────────────────────────────────────────────

    private Product buildProduct(String title, String category, BigDecimal price, int stock)
            throws Exception {
        var ctor = Product.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        var product = ctor.newInstance();
        setField(product, "id", UUID.randomUUID());
        setField(product, "title", title);
        setField(product, "description", "Descrição de " + title);
        setField(product, "price", price);
        setField(product, "stock", stock);
        setField(product, "category", category);
        setField(product, "imageUrl", "https://example.com/img.jpg");
        setField(product, "active", true);
        setField(product, "createdAt", OffsetDateTime.now());
        setField(product, "updatedAt", OffsetDateTime.now());
        return product;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

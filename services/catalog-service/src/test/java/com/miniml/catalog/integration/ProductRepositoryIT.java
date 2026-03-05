package com.miniml.catalog.integration;

import com.miniml.catalog.domain.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de repositório isolado:
 * - Usa @DataJpaTest (só carrega a camada JPA + Flyway)
 * - AutoConfigureTestDatabase(replace = NONE) para não trocar pelo H2
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    ProductRepository productRepository;

    @Test
    void flyway_executaV1eV2_seedCarregado() {
        long total = productRepository.count();
        assertThat(total).isGreaterThanOrEqualTo(12); // V2 insere 12 produtos
    }

    @Test
    void findByActiveTrue_retornaSomenteAtivos() {
        var page = productRepository.findByActiveTrue(PageRequest.of(0, 50));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(p -> p.isActive());
    }

    @Test
    void search_porTitulo_retornaResultados() {
        var page = productRepository.search("iPhone", null, PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().get(0).getTitle()).containsIgnoringCase("iPhone");
    }

    @Test
    void search_porCategoria_retornaSomenteACategoria() {
        var page = productRepository.search(null, "ELECTRONICS", PageRequest.of(0, 50));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(p -> p.getCategory().equalsIgnoreCase("ELECTRONICS"));
    }

    @Test
    void search_categoriaInexistente_retornaListaVazia() {
        var page = productRepository.search(null, "CATEGORIA_INEXISTENTE", PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findByIdAndActiveTrue_idInexistente_retornaEmpty() {
        var result = productRepository.findByIdAndActiveTrue(java.util.UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}

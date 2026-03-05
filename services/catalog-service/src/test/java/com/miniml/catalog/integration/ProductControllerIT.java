package com.miniml.catalog.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração end-to-end:
 * - PostgreSQL via Testcontainers  (@ServiceConnection autoconfigura o datasource)
 * - Flyway roda V1 + V2 (seed de 12 produtos)
 * - JwtDecoder mockado para não precisar de Keycloak rodando
 * - Endpoints públicos, sem autenticação nos testes
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ProductControllerIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    // Substitui o JwtDecoder auto-configurado — endpoints públicos não precisam
    // de JWT válido, mas o bean é necessário para o Spring Security inicializar.
    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    MockMvc mockMvc;

    // ── GET /products ────────────────────────────────────────

    @Test
    void listarProdutos_retornaOkComConteudo() throws Exception {
        mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalElements", greaterThan(0)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.content[0].price").isNotEmpty());
    }

    @Test
    void listarProdutos_filtrarPorCategoria_retornaSomenteACategoria() throws Exception {
        mockMvc.perform(get("/products")
                        .param("category", "ELECTRONICS")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].category", everyItem(is("ELECTRONICS"))));
    }

    @Test
    void listarProdutos_buscarPorTitulo_retornaResultados() throws Exception {
        mockMvc.perform(get("/products")
                        .param("query", "iPhone")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.content[0].title", containsStringIgnoringCase("iPhone")));
    }

    @Test
    void listarProdutos_paginacao_respeitaTamanho() throws Exception {
        mockMvc.perform(get("/products")
                        .param("page", "0")
                        .param("size", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(3))))
                .andExpect(jsonPath("$.size").value(3));
    }

    // ── GET /products/{id} ───────────────────────────────────

    @Test
    void buscarPorId_idInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/products/00000000-0000-0000-0000-000000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Produto não encontrado"));
    }

    @Test
    void buscarPorId_uuidMalformado_retorna400() throws Exception {
        mockMvc.perform(get("/products/not-a-valid-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── POST /products ────────────────────────────────────────

    @Test
    void criarProduto_comRoleAdmin_retorna201() throws Exception {
        String body = """
                {
                  "title": "Produto Teste IT",
                  "description": "Desc",
                  "price": 99.99,
                  "stock": 10,
                  "category": "ELECTRONICS"
                }
                """;

        mockMvc.perform(post("/products")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Produto Teste IT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(header().exists("Location"));
    }

    @Test
    void criarProduto_semAutenticacao_retorna401() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"price\":1,\"stock\":0,\"category\":\"C\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criarProduto_comRoleUser_retorna403() throws Exception {
        mockMvc.perform(post("/products")
                        .with(jwt().authorities(() -> "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"price\":1,\"stock\":0,\"category\":\"C\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void criarProduto_payloadInvalido_retorna400() throws Exception {
        String body = """
                { "title": "", "price": -1, "stock": -5, "category": "" }
                """;
        mockMvc.perform(post("/products")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /products/{id} ────────────────────────────────────

    @Test
    void atualizarProduto_comRoleAdmin_retorna200() throws Exception {
        // Cria o produto primeiro
        String createBody = """
                {"title":"Para Atualizar","description":"D","price":10.0,"stock":5,"category":"BOOKS"}
                """;
        var result = mockMvc.perform(post("/products")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        var id = com.jayway.jsonpath.JsonPath.<String>read(
                result.getResponse().getContentAsString(), "$.id");

        String updateBody = """
                {"title":"Atualizado","description":"Nova desc","price":19.99,"stock":20,"category":"BOOKS"}
                """;
        mockMvc.perform(put("/products/{id}", id)
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Atualizado"))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void atualizarProduto_naoExistente_retorna404() throws Exception {
        String body = """
                {"title":"X","price":1,"stock":0,"category":"C"}
                """;
        mockMvc.perform(put("/products/00000000-0000-0000-0000-000000000000")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /products/{id} ─────────────────────────────────

    @Test
    void deletarProduto_comRoleAdmin_retorna204() throws Exception {
        // Cria primeiro
        String createBody = """
                {"title":"Para Deletar","price":5.0,"stock":1,"category":"OTHER"}
                """;
        var result = mockMvc.perform(post("/products")
                        .with(jwt().authorities(() -> "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        var id = com.jayway.jsonpath.JsonPath.<String>read(
                result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(delete("/products/{id}", id)
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        // Confirma que sumiu do GET
        mockMvc.perform(get("/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletarProduto_semPermissao_retorna403() throws Exception {
        mockMvc.perform(delete("/products/00000000-0000-0000-0000-000000000000")
                        .with(jwt().authorities(() -> "ROLE_USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarPorId_produtoExistente_retornaDetalhe() throws Exception {
        // Primeiro busca a lista para pegar um ID real
        var listResult = mockMvc.perform(get("/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        // Extrai o primeiro ID da lista via regex simples
        var matcher = java.util.regex.Pattern
                .compile("\"id\":\"([0-9a-f-]{36})\"")
                .matcher(body);

        if (matcher.find()) {
            String id = matcher.group(1);
            mockMvc.perform(get("/products/" + id).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.title").isNotEmpty())
                    .andExpect(jsonPath("$.price").isNumber());
        }
    }
}

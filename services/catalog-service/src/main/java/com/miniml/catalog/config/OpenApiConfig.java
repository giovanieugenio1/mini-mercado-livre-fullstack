package com.miniml.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:8079/api/v1").description("Via API Gateway"),
                        new Server().url("http://localhost:8081").description("Direto (dev)")
                ))
                .info(new Info()
                        .title("Catalog Service API")
                        .description("API REST do catálogo de produtos do Mini Mercado Livre. " +
                                     "Leitura pública; escrita exige ROLE_ADMIN.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido via Keycloak (realm mini-mercado-livre)")));
    }
}

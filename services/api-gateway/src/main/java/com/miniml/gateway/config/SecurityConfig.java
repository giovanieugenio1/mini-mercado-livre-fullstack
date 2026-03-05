package com.miniml.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public CorsWebFilter corsWebFilter() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of(
                                "http://localhost:4200", "http://localhost:4201",
                                "http://localhost:8079", "http://localhost:8081", "http://localhost:8082",
                                "http://localhost:8083", "http://localhost:8084", "http://localhost:8085",
                                "http://localhost:8086", "http://localhost:8087"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return new CorsWebFilter(source);
        }

        @Bean
        public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                                .cors(Customizer.withDefaults())
                                .authorizeExchange(auth -> auth
                                                .pathMatchers("/actuator/health", "/actuator/info",
                                                                "/actuator/prometheus")
                                                .permitAll()
                                                .pathMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                                "/v3/api-docs/**", "/api-docs/**", "/webjars/**")
                                                .permitAll()
                                                .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                                                .anyExchange().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter())))
                                .build();
        }

        @Bean
        public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
                var authoritiesConverter = new JwtGrantedAuthoritiesConverter();
                authoritiesConverter.setAuthoritiesClaimName("roles");
                authoritiesConverter.setAuthorityPrefix("");

                var converter = new ReactiveJwtAuthenticationConverter();
                converter.setJwtGrantedAuthoritiesConverter(
                                jwt -> Flux.fromIterable(authoritiesConverter.convert(jwt)));
                return converter;
        }
}

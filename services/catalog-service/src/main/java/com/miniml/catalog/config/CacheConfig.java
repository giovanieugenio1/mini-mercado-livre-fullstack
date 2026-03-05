package com.miniml.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.catalog.dto.ProductResponse;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

        /**
         * RedisCacheManager com:
         * - Cache "products": serializer tipado para ProductResponse (sem @class no
         * JSON)
         * - TTL de 30 minutos
         * - Chaves como String, valores como JSON via ObjectMapper do Spring
         * - Não cacheia valores nulos
         */
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory cf, ObjectMapper objectMapper) {
                var keySerializer = RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer());
                var productSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, ProductResponse.class);
                var productsConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .disableCachingNullValues()
                                .serializeKeysWith(keySerializer)
                                .serializeValuesWith(
                                                RedisSerializationContext.SerializationPair
                                                                .fromSerializer(productSerializer));

                return RedisCacheManager.builder(cf)
                                .withCacheConfiguration("products", productsConfig)
                                .build();
        }
}

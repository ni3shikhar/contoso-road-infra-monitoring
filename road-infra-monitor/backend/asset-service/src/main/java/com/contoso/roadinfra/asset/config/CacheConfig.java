package com.contoso.roadinfra.asset.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for asset-service.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Configure ObjectMapper with Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL);
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Assets cache - longer TTL since assets don't change frequently
        cacheConfigurations.put("assets", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Asset by code cache
        cacheConfigurations.put("assetsByCode", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Children cache
        cacheConfigurations.put("assetChildren", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Corridor summary - shorter TTL as it aggregates changing data
        cacheConfigurations.put("corridorSummary", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // GeoJSON cache
        cacheConfigurations.put("geoJson", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Inspections cache
        cacheConfigurations.put("inspections", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Milestones cache
        cacheConfigurations.put("milestones", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}

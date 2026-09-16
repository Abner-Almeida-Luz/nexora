package com.diversao.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache em memória simples. ConcurrentMapCacheManager não tem TTL nem limite
 * de tamanho — para o volume atual é suficiente. Se virar gargalo, trocar
 * por Caffeine (mesma API, +Ttl +máximo de entradas).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS = "products";
    public static final String CATEGORIES = "categories";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(PRODUCTS, CATEGORIES);
    }
}
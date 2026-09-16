package com.diversao.backend.it;

import com.diversao.backend.user.Role;
import com.diversao.backend.user.User;
import com.diversao.backend.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base para testes de integração via MockMvc contra Postgres real (Testcontainers).
 *
 * - Container Postgres iniciado uma única vez via static initializer: como
 *   todas as subclasses compartilham esta classe-base, o bloco roda só uma vez
 *   por JVM de teste. O Testcontainers registra um shutdown hook (Ryuk) que
 *   derruba o container no fim.
 * - @Transactional garante rollback por teste.
 * - Caches do Spring (item 6) são limpos entre testes: cache não é
 *   transacional, então dado cacheado em um teste sobreviveria ao rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected CacheManager cacheManager;

    private static final AtomicInteger SEQ = new AtomicInteger();

    @BeforeEach
    void clearCaches() {
        // Cache não é transacional: sem isso, dados cacheados por um teste
        // sobreviveriam ao rollback e poluiriam o teste seguinte.
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }

    protected String uniqueSuffix() {
        return "t" + SEQ.incrementAndGet() + "-" + System.nanoTime();
    }

    protected String registerUserAndGetToken(String password) throws Exception {
        String email = "user-" + uniqueSuffix() + "@nexora.test";
        var body = Map.of("name", "User Test", "email", email, "password", password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        return login(email, password);
    }

    protected String createAdminAndGetToken(String password) throws Exception {
        String email = "admin-" + uniqueSuffix() + "@nexora.test";
        User admin = User.builder()
                .name("Admin Test")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .build();
        userRepository.saveAndFlush(admin);
        return login(email, password);
    }

    protected String login(String email, String password) throws Exception {
        var body = Map.of("email", email, "password", password);
        var mvc = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(mvc.getResponse().getContentAsString())
                .get("token").asText();
    }
}
package com.diversao.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto completo só para checar que o wiring está íntegro.
 * Precisa do @ActiveProfiles("test") explicitamente: sem isso o Spring cai
 * no spring.profiles.active do application.yaml (dev), que aponta para
 * Postgres real — e o teste quebra por timeout de conexão, não por bug de código.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}
}
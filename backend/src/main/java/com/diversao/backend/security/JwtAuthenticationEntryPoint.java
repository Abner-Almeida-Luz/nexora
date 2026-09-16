package com.diversao.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Devolve 401 (não 403) quando a requisição não tem autenticação nenhuma —
 * distinto de AccessDeniedHandler, que trata 403 para quem está autenticado
 * mas sem permissão (ex.: USER tentando acessar rota ADMIN).
 *
 * Usa o ObjectMapper gerenciado pelo Spring (com JSR-310 registrado) em vez
 * de `new ObjectMapper()`: sem o módulo JSR-310, serializar `Instant.now()`
 * estoura InvalidDefinitionException e o entry point falha em runtime.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource");
        pd.setTitle("Unauthorized");
        pd.setProperty("timestamp", Instant.now());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
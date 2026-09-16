package com.diversao.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Auditoria leve via SLF4J. Formato pensado para grep:
 *   AUDIT action=PRODUCT_DELETE adminEmail=admin@x.com targetId=42
 * Sem tabela dedicada por enquanto. Se compliance exigir retenção
 * estruturada, migrar para uma tabela + listener de eventos.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void log(String action, Object targetId) {
        log.info("AUDIT action={} adminEmail={} targetId={}",
                action, currentPrincipal(), targetId);
    }

    private String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Em testes unitários (sem contexto) ou jobs internos, fica "system".
        return auth != null ? auth.getName() : "system";
    }
}
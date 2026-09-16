package com.diversao.backend.security;

import com.diversao.backend.exception.ErrorMessages;
import com.diversao.backend.exception.TooManyRequestsException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter de tentativas de login por e-mail (não por IP — evita punir
 * usuários atrás de NAT/redes compartilhadas). Em memória por instância;
 * em múltiplas réplicas cada uma tem seu próprio contador (aceitável para
 * MVP; a solução seria Redis com bucket4j-redis).
 *
 * O mapa cresce com e-mails distintos e nunca é limpo — sem TTL. Para o
 * volume atual, é irrelevante; se virar problema, trocar por Caffeine.
 */
@Component
public class LoginRateLimiter {

    private static final int CAPACITY = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Consome 1 token; estoura com 429 se o bucket estiver vazio. */
    public void checkAllowed(String email) {
        Bucket bucket = buckets.computeIfAbsent(email, k -> newBucket());
        if (!bucket.tryConsume(1)) {
            throw new TooManyRequestsException(ErrorMessages.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    /** Usado por testes para não vazar estado entre casos que reusam o mesmo e-mail. */
    public void reset(String email) {
        buckets.remove(email);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, WINDOW));
        return Bucket.builder().addLimit(limit).build();
    }
}
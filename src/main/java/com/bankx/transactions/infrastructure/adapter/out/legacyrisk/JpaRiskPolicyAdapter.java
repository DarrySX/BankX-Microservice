package com.bankx.transactions.infrastructure.adapter.out.legacyrisk;

import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
@Slf4j
public class JpaRiskPolicyAdapter implements RiskPolicyPort {

    /**
     * Límite conservador que se aplica cuando el módulo legado no responde: preferimos rechazar
     * de más a aprobar a ciegas. Nunca es más permisivo que la regla más estricta de la tabla.
     */
    private static final BigDecimal DEGRADED_MAX_DEBIT = new BigDecimal("100");

    /** Un H2 que tarde más que esto se considera caído: mejor degradar que acumular hilos. */
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(2);
    private static final long RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofMillis(200);
    private static final double RETRY_JITTER = 0.5;

    private final RiskRuleJpaRepository repository;

    @Override
    public Mono<Boolean> isAllowed(String currency, TransactionType type, BigDecimal amount) {
        if (type != TransactionType.DEBIT) {
            return Mono.just(true);               // los créditos no pasan por riesgo
        }
        return Mono.fromCallable(() -> repository.findFirstByCurrency(currency)
                        .map(RiskRule::getMaxDebitPerTx)
                        .orElse(BigDecimal.ZERO))
                .subscribeOn(Schedulers.boundedElastic())   // ⬅️ NUNCA en el event-loop
                .timeout(QUERY_TIMEOUT)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, RETRY_BACKOFF).jitter(RETRY_JITTER))
                .map(max -> amount.compareTo(max) <= 0)
                .onErrorResume(ex -> degradedPolicy(currency, amount, ex));
    }

    /**
     * Agotados los reintentos, el servicio sigue operativo con una política reducida
     * en lugar de propagar un 500. Se registra en WARN para que quede rastro en la operación.
     */
    private Mono<Boolean> degradedPolicy(String currency, BigDecimal amount, Throwable ex) {
        log.warn("Módulo de riesgo no disponible para {}, aplicando política degradada (máx {})",
                currency, DEGRADED_MAX_DEBIT, ex);
        return Mono.just(amount.compareTo(DEGRADED_MAX_DEBIT) <= 0);
    }
}

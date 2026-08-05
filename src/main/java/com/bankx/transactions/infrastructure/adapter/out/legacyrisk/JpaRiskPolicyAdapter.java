package com.bankx.transactions.infrastructure.adapter.out.legacyrisk;

import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
public class JpaRiskPolicyAdapter implements RiskPolicyPort {

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
                .timeout(Duration.ofSeconds(2))             // un H2 lento no acumula hilos
                .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).jitter(0.5))
                .map(max -> amount.compareTo(max) <= 0);
    }
}

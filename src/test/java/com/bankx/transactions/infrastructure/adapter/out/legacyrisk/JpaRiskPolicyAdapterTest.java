package com.bankx.transactions.infrastructure.adapter.out.legacyrisk;

import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

/**
 * Integración real contra H2 a través del puente bloqueante.
 * Perfil {@code test}: el {@code DataSeeder} queda fuera, así que el test siembra sus propias reglas.
 */
@SpringBootTest
@ActiveProfiles("test")
class JpaRiskPolicyAdapterTest {

    @Autowired
    RiskPolicyPort riskPolicy;

    @Autowired
    RiskRuleJpaRepository riskRepository;

    @BeforeEach
    void seedRules() {
        riskRepository.deleteAll();
        riskRepository.save(RiskRule.builder().currency("PEN").maxDebitPerTx(new BigDecimal("1500")).build());
    }

    @Test
    void allowsDebitUnderLimit() {
        StepVerifier.create(riskPolicy.isAllowed("PEN", TransactionType.DEBIT, new BigDecimal("100")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void deniesDebitOverLimit() {
        StepVerifier.create(riskPolicy.isAllowed("PEN", TransactionType.DEBIT, new BigDecimal("5000")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void creditsSkipRiskEvaluation() {
        StepVerifier.create(riskPolicy.isAllowed("PEN", TransactionType.CREDIT, new BigDecimal("999999")))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void deniesWhenCurrencyHasNoRule() {
        StepVerifier.create(riskPolicy.isAllowed("EUR", TransactionType.DEBIT, new BigDecimal("1")))
                .expectNext(false)
                .verifyComplete();
    }
}

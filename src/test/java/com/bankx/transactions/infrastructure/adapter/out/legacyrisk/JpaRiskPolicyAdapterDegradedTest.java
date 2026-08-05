package com.bankx.transactions.infrastructure.adapter.out.legacyrisk;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.model.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import reactor.test.StepVerifier;

/** Política degradada: el módulo legado está caído y el servicio sigue respondiendo. */
class JpaRiskPolicyAdapterDegradedTest {

    private final RiskRuleJpaRepository repository = mock(RiskRuleJpaRepository.class);
    private final JpaRiskPolicyAdapter adapter = new JpaRiskPolicyAdapter(repository);

    @Test
    void allowsSmallDebitWhenLegacyModuleIsDown() {
        when(repository.findFirstByCurrency(anyString()))
                .thenThrow(new DataAccessResourceFailureException("H2 caído"));

        StepVerifier.create(adapter.isAllowed("PEN", TransactionType.DEBIT, new BigDecimal("50")))
                .expectNext(true)
                .verifyComplete();

        // se reintentó antes de degradar: 1 intento + reintentos con backoff
        verify(repository, atLeast(2)).findFirstByCurrency("PEN");
    }

    @Test
    void deniesLargeDebitWhenLegacyModuleIsDown() {
        when(repository.findFirstByCurrency(anyString()))
                .thenThrow(new DataAccessResourceFailureException("H2 caído"));

        StepVerifier.create(adapter.isAllowed("PEN", TransactionType.DEBIT, new BigDecimal("1000")))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void neverTouchesTheLegacyModuleForCredits() {
        StepVerifier.create(adapter.isAllowed("PEN", TransactionType.CREDIT, new BigDecimal("999999")))
                .expectNext(true)
                .verifyComplete();

        verify(repository, never()).findFirstByCurrency(anyString());
    }
}

package com.bankx.transactions.infrastructure.adapter.out.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.AccountDocument;
import com.bankx.transactions.infrastructure.adapter.out.mongo.repository.AccountMongoRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class MongoAccountAdapterTest {

    private final AccountMongoRepository repository = mock(AccountMongoRepository.class);
    private final MongoAccountAdapter adapter = new MongoAccountAdapter(repository);

    private static AccountDocument document() {
        return AccountDocument.builder()
                .id("id-1").number("001-0001").holderName("Ana Peru")
                .currency("PEN").balance(new BigDecimal("2000"))
                .build();
    }

    @Test
    void findByNumberMapsTheDocumentToDomain() {
        when(repository.findByNumber("001-0001")).thenReturn(Mono.just(document()));

        StepVerifier.create(adapter.findByNumber("001-0001"))
                .expectNext(new Account("id-1", "001-0001", "Ana Peru", "PEN", new BigDecimal("2000")))
                .verifyComplete();
    }

    /** Contrato del puerto (LSP): cuenta inexistente es Mono.empty(), no null ni excepción. */
    @Test
    void findByNumberIsEmptyWhenTheAccountDoesNotExist() {
        when(repository.findByNumber("999-9999")).thenReturn(Mono.empty());

        StepVerifier.create(adapter.findByNumber("999-9999"))
                .verifyComplete();
    }

    @Test
    void saveReturnsTheStoredAccount() {
        when(repository.save(any(AccountDocument.class))).thenReturn(Mono.just(document()));

        var toSave = new Account(null, "001-0001", "Ana Peru", "PEN", new BigDecimal("2000"));

        StepVerifier.create(adapter.save(toSave))
                .assertNext(saved -> assertThat(saved.id()).isEqualTo("id-1"))
                .verifyComplete();
    }
}

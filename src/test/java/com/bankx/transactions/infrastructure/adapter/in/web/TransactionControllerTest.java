package com.bankx.transactions.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankx.transactions.domain.exception.AccountNotFoundException;
import com.bankx.transactions.domain.exception.InsufficientFundsException;
import com.bankx.transactions.domain.exception.RiskRejectedException;
import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.in.CreateTransactionCommand;
import com.bankx.transactions.domain.port.in.CreateTransactionUseCase;
import com.bankx.transactions.domain.port.in.ListTransactionsUseCase;
import com.bankx.transactions.domain.port.in.StreamTransactionsUseCase;
import com.bankx.transactions.infrastructure.adapter.in.web.dto.TransactionResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreateTransactionUseCase createUseCase;

    @MockBean
    private ListTransactionsUseCase listUseCase;

    @MockBean
    private StreamTransactionsUseCase streamUseCase;

    private static Transaction approved() {
        return Transaction.approved("acc-1", TransactionType.DEBIT, new BigDecimal("100")).withId("tx-1");
    }

    private WebTestClient.ResponseSpec post(String body) {
        return webTestClient.post().uri("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    @Test
    void createReturns201AndTheStoredTransaction() {
        when(createUseCase.create(any())).thenReturn(Mono.just(approved()));

        post("{\"accountNumber\":\"001-0001\",\"type\":\"DEBIT\",\"amount\":100}")
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo("tx-1")
                .jsonPath("$.accountId").isEqualTo("acc-1")
                .jsonPath("$.type").isEqualTo("DEBIT")
                .jsonPath("$.status").isEqualTo("OK");
    }

    @Test
    void createMapsTheRequestIntoACommand() {
        when(createUseCase.create(any())).thenReturn(Mono.just(approved()));

        post("{\"accountNumber\":\"001-0001\",\"type\":\"debit\",\"amount\":100}")
                .expectStatus().isCreated();

        var captor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(createUseCase).create(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TransactionType.DEBIT);
        assertThat(captor.getValue().accountNumber()).isEqualTo("001-0001");
    }

    @Test
    void createTranslatesRiskRejection() {
        when(createUseCase.create(any())).thenReturn(Mono.error(new RiskRejectedException()));

        post("{\"accountNumber\":\"001-0001\",\"type\":\"DEBIT\",\"amount\":2000}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("risk_rejected");
    }

    @Test
    void createTranslatesInsufficientFunds() {
        when(createUseCase.create(any())).thenReturn(Mono.error(new InsufficientFundsException()));

        post("{\"accountNumber\":\"001-0002\",\"type\":\"DEBIT\",\"amount\":1200}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("insufficient_funds");
    }

    @Test
    void createTranslatesUnknownAccount() {
        when(createUseCase.create(any())).thenReturn(Mono.error(new AccountNotFoundException()));

        post("{\"accountNumber\":\"999-9999\",\"type\":\"CREDIT\",\"amount\":50}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("account_not_found");
    }

    @Test
    void createRejectsAnInvalidPayloadBeforeReachingTheUseCase() {
        post("{\"accountNumber\":\"\",\"type\":\"TRANSFER\",\"amount\":0}")
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("validation_error");

        verify(createUseCase, never()).create(any());
    }

    @Test
    void listReturnsTheTransactionsOfTheAccount() {
        when(listUseCase.byAccountNumber("001-0001")).thenReturn(Flux.just(approved()));

        webTestClient.get().uri("/api/transactions?accountNumber=001-0001")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TransactionResponse.class).hasSize(1);
    }

    @Test
    void listTranslatesUnknownAccount() {
        when(listUseCase.byAccountNumber("999-9999"))
                .thenReturn(Flux.error(new AccountNotFoundException()));

        webTestClient.get().uri("/api/transactions?accountNumber=999-9999")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.error").isEqualTo("account_not_found");
    }

    @Test
    void streamEmitsServerSentEvents() {
        when(streamUseCase.stream()).thenReturn(Flux.just(approved()));

        webTestClient.get().uri("/api/stream/transactions")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("event:transaction")
                        .contains("tx-1"));
    }
}

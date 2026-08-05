package com.bankx.transactions.infrastructure.adapter.in.web;

import com.bankx.transactions.domain.model.TransactionType;
import com.bankx.transactions.domain.port.in.CreateTransactionCommand;
import com.bankx.transactions.domain.port.in.CreateTransactionUseCase;
import com.bankx.transactions.domain.port.in.ListTransactionsUseCase;
import com.bankx.transactions.domain.port.in.StreamTransactionsUseCase;
import com.bankx.transactions.infrastructure.adapter.in.web.dto.CreateTxRequest;
import com.bankx.transactions.infrastructure.adapter.in.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionController {

    private final CreateTransactionUseCase createUseCase;
    private final ListTransactionsUseCase listUseCase;
    private final StreamTransactionsUseCase streamUseCase;

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TransactionResponse> create(@Valid @RequestBody CreateTxRequest req) {
        var command = new CreateTransactionCommand(
                req.getAccountNumber(), TransactionType.from(req.getType()), req.getAmount());
        return createUseCase.create(command).map(TransactionResponse::from);
    }

    @GetMapping("/transactions")
    public Flux<TransactionResponse> list(@RequestParam String accountNumber) {
        return listUseCase.byAccountNumber(accountNumber).map(TransactionResponse::from);
    }

    @GetMapping(value = "/stream/transactions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TransactionResponse>> stream() {
        return streamUseCase.stream()
                .map(t -> ServerSentEvent.builder(TransactionResponse.from(t))
                        .event("transaction")
                        .build());
    }
}

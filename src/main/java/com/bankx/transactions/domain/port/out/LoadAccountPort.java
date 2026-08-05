package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Account;
import reactor.core.publisher.Mono;

public interface LoadAccountPort {

    /** Contrato (LSP): {@code Mono.empty()} cuando la cuenta no existe. Nunca null ni excepción. */
    Mono<Account> findByNumber(String number);
}

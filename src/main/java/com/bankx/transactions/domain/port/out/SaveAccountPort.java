package com.bankx.transactions.domain.port.out;

import com.bankx.transactions.domain.model.Account;
import reactor.core.publisher.Mono;

public interface SaveAccountPort {
    Mono<Account> save(Account account);
}

package com.bankx.transactions.infrastructure.adapter.out.mongo;

import com.bankx.transactions.domain.model.Account;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.SaveAccountPort;
import com.bankx.transactions.infrastructure.adapter.out.mongo.mapper.AccountMapper;
import com.bankx.transactions.infrastructure.adapter.out.mongo.repository.AccountMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoAccountAdapter implements LoadAccountPort, SaveAccountPort {

    private final AccountMongoRepository repository;

    @Override
    public Mono<Account> findByNumber(String number) {
        return repository.findByNumber(number).map(AccountMapper::toDomain);
    }

    @Override
    public Mono<Account> save(Account account) {
        return repository.save(AccountMapper.toDocument(account)).map(AccountMapper::toDomain);
    }
}

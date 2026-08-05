package com.bankx.transactions.infrastructure.adapter.out.mongo;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.port.out.LoadTransactionsPort;
import com.bankx.transactions.domain.port.out.SaveTransactionPort;
import com.bankx.transactions.infrastructure.adapter.out.mongo.mapper.TransactionMapper;
import com.bankx.transactions.infrastructure.adapter.out.mongo.repository.TransactionMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class MongoTransactionAdapter implements SaveTransactionPort, LoadTransactionsPort {

    private final TransactionMongoRepository repository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.save(TransactionMapper.toDocument(transaction)).map(TransactionMapper::toDomain);
    }

    @Override
    public Flux<Transaction> findByAccountIdOrderByTimestampDesc(String accountId) {
        return repository.findByAccountIdOrderByTimestampDesc(accountId).map(TransactionMapper::toDomain);
    }
}

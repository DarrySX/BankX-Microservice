package com.bankx.transactions.infrastructure.adapter.out.mongo.repository;

import com.bankx.transactions.infrastructure.adapter.out.mongo.document.TransactionDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionMongoRepository extends ReactiveMongoRepository<TransactionDocument, String> {
    Flux<TransactionDocument> findByAccountIdOrderByTimestampDesc(String accountId);
}

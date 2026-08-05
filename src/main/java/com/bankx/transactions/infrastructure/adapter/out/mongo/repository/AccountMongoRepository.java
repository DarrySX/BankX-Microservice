package com.bankx.transactions.infrastructure.adapter.out.mongo.repository;

import com.bankx.transactions.infrastructure.adapter.out.mongo.document.AccountDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface AccountMongoRepository extends ReactiveMongoRepository<AccountDocument, String> {
    Mono<AccountDocument> findByNumber(String number);
}

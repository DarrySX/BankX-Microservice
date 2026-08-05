package com.bankx.transactions.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

/**
 * Acota el scanning de cada tecnología a su propio paquete. Sin esto, Spring Data
 * intenta construir repositorios JPA a partir de las interfaces reactivas y la app no arranca.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.bankx.transactions.infrastructure.adapter.out.legacyrisk")
@EntityScan(basePackages = "com.bankx.transactions.infrastructure.adapter.out.legacyrisk")
@EnableReactiveMongoRepositories(basePackages = "com.bankx.transactions.infrastructure.adapter.out.mongo.repository")
public class PersistenceConfig { }

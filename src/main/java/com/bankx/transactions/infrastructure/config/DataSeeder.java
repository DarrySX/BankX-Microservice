package com.bankx.transactions.infrastructure.config;

import com.bankx.transactions.infrastructure.adapter.out.legacyrisk.RiskRule;
import com.bankx.transactions.infrastructure.adapter.out.legacyrisk.RiskRuleJpaRepository;
import com.bankx.transactions.infrastructure.adapter.out.mongo.document.AccountDocument;
import com.bankx.transactions.infrastructure.adapter.out.mongo.repository.AccountMongoRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Fuera del perfil {@code test}: los tests de integración no deben depender de un Mongo levantado
 * ni de datos precargados; cada uno siembra lo que necesita.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RiskRuleJpaRepository riskRepository;
    private final AccountMongoRepository accountRepository;

    @Override
    public void run(String... args) {
        riskRepository.saveAll(List.of(
                RiskRule.builder().currency("PEN").maxDebitPerTx(new BigDecimal("1500")).build(),
                RiskRule.builder().currency("USD").maxDebitPerTx(new BigDecimal("500")).build()
        ));

        accountRepository.deleteAll()
                .thenMany(Flux.just(
                        AccountDocument.builder().number("001-0001").holderName("Ana Peru")
                                .currency("PEN").balance(new BigDecimal("2000")).build(),
                        AccountDocument.builder().number("001-0002").holderName("Luis Acuña")
                                .currency("PEN").balance(new BigDecimal("800")).build()
                ))
                .flatMap(accountRepository::save)
                .doOnComplete(() -> log.info("Seed completado"))
                .blockLast();   // aceptable SOLO en arranque, fuera del event-loop
    }
}

package com.bankx.transactions.infrastructure.config;

import com.bankx.transactions.application.service.CreateTransactionService;
import com.bankx.transactions.application.service.ListTransactionsService;
import com.bankx.transactions.application.service.StreamTransactionsService;
import com.bankx.transactions.domain.port.in.CreateTransactionUseCase;
import com.bankx.transactions.domain.port.in.ListTransactionsUseCase;
import com.bankx.transactions.domain.port.in.StreamTransactionsUseCase;
import com.bankx.transactions.domain.port.out.LoadAccountPort;
import com.bankx.transactions.domain.port.out.LoadTransactionsPort;
import com.bankx.transactions.domain.port.out.PublishTransactionEventPort;
import com.bankx.transactions.domain.port.out.RiskPolicyPort;
import com.bankx.transactions.domain.port.out.SaveAccountPort;
import com.bankx.transactions.domain.port.out.SaveTransactionPort;
import com.bankx.transactions.domain.port.out.SubscribeTransactionEventsPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Todo el wiring vive aquí: la capa de aplicación no lleva anotaciones de Spring
 * y por eso se puede instanciar con un {@code new} puro en los tests.
 */
@Configuration
public class BeanConfig {

    @Bean
    public CreateTransactionUseCase createTransactionUseCase(
            LoadAccountPort loadAccountPort,
            SaveAccountPort saveAccountPort,
            SaveTransactionPort saveTransactionPort,
            RiskPolicyPort riskPolicyPort,
            PublishTransactionEventPort eventPort) {
        return new CreateTransactionService(
                loadAccountPort, saveAccountPort, saveTransactionPort, riskPolicyPort, eventPort);
    }

    @Bean
    public ListTransactionsUseCase listTransactionsUseCase(
            LoadAccountPort loadAccountPort, LoadTransactionsPort loadTransactionsPort) {
        return new ListTransactionsService(loadAccountPort, loadTransactionsPort);
    }

    @Bean
    public StreamTransactionsUseCase streamTransactionsUseCase(SubscribeTransactionEventsPort port) {
        return new StreamTransactionsService(port);
    }
}

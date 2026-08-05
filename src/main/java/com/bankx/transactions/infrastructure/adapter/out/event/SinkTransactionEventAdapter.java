package com.bankx.transactions.infrastructure.adapter.out.event;

import com.bankx.transactions.domain.model.Transaction;
import com.bankx.transactions.domain.port.out.PublishTransactionEventPort;
import com.bankx.transactions.domain.port.out.SubscribeTransactionEventsPort;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

@Component
public class SinkTransactionEventAdapter implements PublishTransactionEventPort, SubscribeTransactionEventsPort {

    // autoCancel=false: que un cliente SSE cierre la pestaña no debe terminar el sink para los demás
    private final Sinks.Many<Transaction> sink =
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);

    @Override
    public void publish(Transaction transaction) {
        sink.tryEmitNext(transaction);
    }

    @Override
    public Flux<Transaction> events() {
        return sink.asFlux();
    }
}

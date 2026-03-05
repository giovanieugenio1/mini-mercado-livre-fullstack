package com.miniml.order.service;

import com.miniml.order.domain.OutboxEvent;
import com.miniml.order.domain.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Outbox Publisher — lê eventos PENDING e os publica no Kafka.
 *
 * Estratégia:
 *  - SELECT ... FOR UPDATE SKIP LOCKED: seguro para múltiplas instâncias
 *  - Lote de até 50 eventos por execução
 *  - Aguarda confirmação do broker (acks=all) antes de marcar SENT
 *  - Em falha, mantém PENDING para próximo ciclo (retry automático)
 */
@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                   KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending =
                outboxEventRepository.findPendingForUpdate(PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        log.debug("Publicando {} evento(s) pendente(s) no Kafka", pending.size());

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate
                        .send(event.getEventType(),
                              event.getAggregateId().toString(),
                              event.getPayloadJson())
                        .get(10, TimeUnit.SECONDS);   // bloqueia até confirmação do broker

                event.markAsSent();
                log.debug("Outbox SENT: id={} tipo={}", event.getId(), event.getEventType());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrompido ao publicar evento {}", event.getId());

            } catch (ExecutionException | TimeoutException e) {
                log.error("Falha ao publicar evento outbox id={} tipo={}",
                          event.getId(), event.getEventType(), e);
                // Mantém PENDING → próximo ciclo tentará novamente
            }
        }
    }
}

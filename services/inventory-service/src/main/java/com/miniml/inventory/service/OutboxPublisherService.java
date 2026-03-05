package com.miniml.inventory.service;

import com.miniml.inventory.domain.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

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
        var pending = outboxEventRepository.findPendingForUpdate(
                PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) return;

        for (var event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(),
                                   event.getAggregateId().toString(),
                                   event.getPayloadJson())
                        .get(10, TimeUnit.SECONDS);

                event.markAsSent();
                log.debug("Evento publicado: type={} aggregateId={}",
                        event.getEventType(), event.getAggregateId());

            } catch (Exception ex) {
                event.markAsFailed();
                log.error("Falha ao publicar evento: type={} id={} erro={}",
                        event.getEventType(), event.getId(), ex.getMessage());
            }
        }
    }
}

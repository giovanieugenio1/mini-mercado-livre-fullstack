package com.miniml.notification.service;

import com.miniml.notification.domain.Notification;
import com.miniml.notification.domain.NotificationRepository;
import com.miniml.notification.domain.ProcessedEvent;
import com.miniml.notification.domain.ProcessedEventRepository;
import com.miniml.notification.dto.NotificationResponse;
import com.miniml.notification.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                ProcessedEventRepository processedEventRepository) {
        this.notificationRepository  = notificationRepository;
        this.processedEventRepository = processedEventRepository;
    }

    // ── Registro de notificação (chamado pelo consumer Kafka) ─────────────────

    /**
     * Persiste a notificação e marca o evento como processado.
     * Idempotente: ignora eventId já registrado em processed_event.
     */
    @Transactional
    public void notify(UUID eventId, String eventType, UUID orderId,
                       UUID customerId, String subject, String body) {
        if (processedEventRepository.existsById(eventId)) {
            log.info("Evento {} já processado — ignorando (idempotência)", eventId);
            return;
        }

        var notification = Notification.of(orderId, customerId, eventType, subject, body);
        notificationRepository.save(notification);
        processedEventRepository.save(new ProcessedEvent(eventId, eventType));

        // Mock: log simula envio de e-mail/SMS
        log.info("[NOTIFICAÇÃO MOCK] canal=EMAIL para={} assunto='{}' evento={}",
                customerId, subject, eventType);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public PageResponse<NotificationResponse> listAll(int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return PageResponse.from(notificationRepository.findAll(pageable),
                NotificationResponse::from);
    }

    public PageResponse<NotificationResponse> listByOrderId(UUID orderId, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return PageResponse.from(
                notificationRepository.findByOrderId(orderId, pageable),
                NotificationResponse::from);
    }

    public PageResponse<NotificationResponse> listByCustomerId(UUID customerId, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return PageResponse.from(
                notificationRepository.findByCustomerId(customerId, pageable),
                NotificationResponse::from);
    }
}

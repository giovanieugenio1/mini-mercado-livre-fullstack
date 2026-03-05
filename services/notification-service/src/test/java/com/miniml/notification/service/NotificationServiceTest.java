package com.miniml.notification.service;

import com.miniml.notification.domain.Notification;
import com.miniml.notification.domain.NotificationRepository;
import com.miniml.notification.domain.ProcessedEvent;
import com.miniml.notification.domain.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock ProcessedEventRepository processedEventRepository;

    NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository, processedEventRepository);
    }

    @Test
    void devePersistirNotificacaoParaEventoNaoProcessado() {
        var eventId    = UUID.randomUUID();
        var orderId    = UUID.randomUUID();
        var customerId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        service.notify(eventId, "order.created.v1", orderId, customerId,
                "Pedido criado!", "Corpo da notificação");

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Pedido criado!");
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customerId);
        assertThat(captor.getValue().getEventType()).isEqualTo("order.created.v1");
        assertThat(captor.getValue().getStatus()).isEqualTo("SENT");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void deveIgnorarEventoJaProcessado() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.notify(eventId, "order.created.v1", UUID.randomUUID(),
                UUID.randomUUID(), "Subject", "Body");

        verifyNoInteractions(notificationRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void devePermitirNotificacaoSemOrderId() {
        // Alguns eventos podem não ter orderId (ex: notificação de sistema)
        var eventId    = UUID.randomUUID();
        var customerId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        service.notify(eventId, "system.alert.v1", null, customerId,
                "Alerta", "Mensagem de sistema");

        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isNull();
    }

    @Test
    void deveRegistrarEventosDistintosDoMesmoPedido() {
        var orderId    = UUID.randomUUID();
        var customerId = UUID.randomUUID();

        when(processedEventRepository.existsById(any())).thenReturn(false);

        service.notify(UUID.randomUUID(), "order.created.v1", orderId, customerId, "S1", "B1");
        service.notify(UUID.randomUUID(), "payment.authorized.v1", orderId, customerId, "S2", "B2");
        service.notify(UUID.randomUUID(), "shipping.created.v1", orderId, customerId, "S3", "B3");

        verify(notificationRepository, times(3)).save(any());
    }
}

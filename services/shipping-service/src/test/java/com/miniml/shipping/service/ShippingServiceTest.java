package com.miniml.shipping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.shipping.domain.*;
import com.miniml.shipping.exception.ShippingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

    @Mock ShippingRepository shippingRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ProcessedEventRepository processedEventRepository;

    ShippingService service;

    @BeforeEach
    void setUp() {
        service = new ShippingService(shippingRepository, outboxEventRepository,
                processedEventRepository, new ObjectMapper());
    }

    // ── processInventoryReserved ───────────────────────────────────────────────

    @Test
    void deveCriarEnvioQuandoEventoNaoProcessado() {
        var eventId    = UUID.randomUUID();
        var orderId    = UUID.randomUUID();
        var customerId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(shippingRepository.save(any(Shipping.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.processInventoryReserved(eventId, orderId, customerId, orderId);

        var shippingCaptor = ArgumentCaptor.forClass(Shipping.class);
        verify(shippingRepository).save(shippingCaptor.capture());
        assertThat(shippingCaptor.getValue().getStatus()).isEqualTo("CREATED");
        assertThat(shippingCaptor.getValue().getTrackingCode()).startsWith("MINIML");
        assertThat(shippingCaptor.getValue().getOrderId()).isEqualTo(orderId);

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("shipping.created.v1");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void deveIgnorarEventoJaProcessado() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.processInventoryReserved(eventId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());

        verifyNoInteractions(shippingRepository, outboxEventRepository);
    }

    @Test
    void deveCriarTrackingCodeUnico() {
        // Dois envios devem gerar tracking codes distintos
        var eventId1 = UUID.randomUUID();
        var eventId2 = UUID.randomUUID();

        when(processedEventRepository.existsById(any())).thenReturn(false);
        when(shippingRepository.save(any(Shipping.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.processInventoryReserved(eventId1, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        service.processInventoryReserved(eventId2, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        var captor = ArgumentCaptor.forClass(Shipping.class);
        verify(shippingRepository, times(2)).save(captor.capture());

        var codes = captor.getAllValues().stream().map(Shipping::getTrackingCode).toList();
        assertThat(codes.get(0)).isNotEqualTo(codes.get(1));
    }

    // ── findByOrderId ─────────────────────────────────────────────────────────

    @Test
    void deveLancarExcecaoQuandoEnvioNaoEncontrado() {
        var orderId = UUID.randomUUID();
        when(shippingRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByOrderId(orderId))
                .isInstanceOf(ShippingNotFoundException.class);
    }
}

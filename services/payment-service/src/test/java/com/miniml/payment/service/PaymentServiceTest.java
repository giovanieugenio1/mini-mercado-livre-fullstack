package com.miniml.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.payment.domain.*;
import com.miniml.payment.exception.PaymentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ProcessedEventRepository processedEventRepository;

    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, outboxEventRepository,
                processedEventRepository, new ObjectMapper());
        // Injeta o valor da @Value via ReflectionTestUtils
        ReflectionTestUtils.setField(service, "maxAuthorizedAmount", new BigDecimal("50000.00"));
    }

    // ── processOrderCreated ───────────────────────────────────────────────────

    @Test
    void deveAutorizarPagamentoQuandoValorDentroDoLimite() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var amount = new BigDecimal("9999.99");

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        service.processOrderCreated(eventId, orderId, customerId, amount, "[]", orderId);

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("payment.authorized.v1");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void deveRecusarPagamentoQuandoValorAcimaDoLimite() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var customerId = UUID.randomUUID();
        var amount = new BigDecimal("99999.99");

        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        service.processOrderCreated(eventId, orderId, customerId, amount, "[]", orderId);

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(captor.getValue().getFailureReason()).isNotNull();

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("payment.failed.v1");
    }

    @Test
    void deveAutorizarPagamentoNoLimiteExato() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);

        service.processOrderCreated(eventId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("50000.00"), "[]", UUID.randomUUID());

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void deveIgnorarEventoJaProcessado() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.processOrderCreated(eventId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100.00"), "[]", UUID.randomUUID());

        verifyNoInteractions(paymentRepository, outboxEventRepository);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontradoPorId() {
        var id = UUID.randomUUID();
        when(paymentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ── findByOrderId ─────────────────────────────────────────────────────────

    @Test
    void deveLancarExcecaoQuandoPagamentoNaoEncontradoPorOrderId() {
        var orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByOrderId(orderId))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void deveRetornarPaginaVaziaQuandoNaoHaPagamentos() {
        when(paymentRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var result = service.list(null, null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }
}

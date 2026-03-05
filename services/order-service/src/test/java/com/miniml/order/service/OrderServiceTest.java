package com.miniml.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.order.domain.*;
import com.miniml.order.dto.CreateOrderRequest;
import com.miniml.order.dto.OrderItemRequest;
import com.miniml.order.exception.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ProcessedEventRepository processedEventRepository;

    @InjectMocks OrderService orderService;

    // ObjectMapper real (não mock) para testar serialização
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @BeforeEach
    void injectObjectMapper() throws Exception {
        var field = OrderService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(orderService, objectMapper);
    }

    // ── createOrder ───────────────────────────────────────────

    @Test
    void createOrder_salvaOrderEOutboxNaMesmaTransacao() {
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var request = buildRequest(UUID.randomUUID(), 2, new BigDecimal("7999.99"));
        var response = orderService.createOrder(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.totalAmount()).isEqualByComparingTo("15999.98");
        assertThat(response.items()).hasSize(1);

        // Deve ter salvo 1 Order e 1 OutboxEvent
        verify(orderRepository).save(any(Order.class));
        verify(outboxEventRepository).save(argThat(e ->
                e.getEventType().equals("order.created.v1") &&
                e.getStatus().equals("PENDING")));
    }

    @Test
    void createOrder_calculaTotalCorretamente() {
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var customerId = UUID.randomUUID();
        var request = new CreateOrderRequest(customerId, List.of(
                new OrderItemRequest(UUID.randomUUID(), "Produto A", new BigDecimal("100.00"), 3),
                new OrderItemRequest(UUID.randomUUID(), "Produto B", new BigDecimal("50.50"), 2)
        ));

        var response = orderService.createOrder(request);

        // 3×100 + 2×50.50 = 300 + 101 = 401
        assertThat(response.totalAmount()).isEqualByComparingTo("401.00");
    }

    // ── findById ──────────────────────────────────────────────

    @Test
    void findById_pedidoExistente_retornaResponse() {
        var order = buildOrder();
        when(orderRepository.findByIdWithItems(order.getId()))
                .thenReturn(Optional.of(order));

        var result = orderService.findById(order.getId());

        assertThat(result.id()).isEqualTo(order.getId());
        assertThat(result.status()).isEqualTo("CREATED");
    }

    @Test
    void findById_pedidoInexistente_lancaNotFoundException() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdWithItems(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── handleExternalEvent (idempotência) ────────────────────

    @Test
    void handleExternalEvent_eventoNovo_atualizaStatus() {
        var order = buildOrder();
        UUID eventId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.handleExternalEvent(eventId, order.getId(), OrderStatus.PAID, "Pago");

        verify(orderRepository, atLeastOnce()).save(order);
        verify(processedEventRepository).save(argThat(pe -> pe.getEventId().equals(eventId)));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void handleExternalEvent_eventoJaProcessado_naoAtualiza() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        orderService.handleExternalEvent(eventId, orderId, OrderStatus.PAID, "Pago");

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleExternalEvent_shippingCreated_publicaOrderCompleted() {
        var order = buildOrder();
        // Transiciona para o estado anterior ao SHIPPING_CREATED
        order.transition(OrderStatus.INVENTORY_RESERVED, "reservado");

        UUID eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.handleExternalEvent(eventId, order.getId(),
                OrderStatus.SHIPPING_CREATED, "Enviado");

        verify(outboxEventRepository).save(argThat(e ->
                e.getEventType().equals("order.completed.v1")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    // ── Helper ────────────────────────────────────────────────

    private Order buildOrder() {
        return Order.create(UUID.randomUUID(), List.of(
                new Order.OrderItemData(UUID.randomUUID(), "iPhone 15",
                        new BigDecimal("7999.99"), 1)));
    }

    private CreateOrderRequest buildRequest(UUID customerId, int qty, BigDecimal price) {
        return new CreateOrderRequest(customerId, List.of(
                new OrderItemRequest(UUID.randomUUID(), "iPhone 15 Pro", price, qty)));
    }
}

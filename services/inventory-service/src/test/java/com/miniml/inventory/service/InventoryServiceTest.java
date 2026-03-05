package com.miniml.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniml.inventory.domain.*;
import com.miniml.inventory.exception.ProductStockNotFoundException;
import com.miniml.inventory.exception.ReservationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock ProductStockRepository productStockRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    InventoryService service;

    static final UUID PRODUCT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID PRODUCT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    static final String ITEMS_JSON = """
            [{"productId":"%s","quantity":2},{"productId":"%s","quantity":1}]
            """.formatted(PRODUCT_A, PRODUCT_B);

    @BeforeEach
    void setUp() {
        service = new InventoryService(productStockRepository, reservationRepository,
                outboxEventRepository, processedEventRepository, redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "lockTtlSeconds", 30L);

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    // ── reserveStock ──────────────────────────────────────────────────────────

    @Test
    void deveReservarEstoqueQuandoDisponivel() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var stockA  = ProductStock.of(PRODUCT_A, 100);
        var stockB  = ProductStock.of(PRODUCT_B, 50);

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(productStockRepository.findByIdForUpdate(PRODUCT_A)).thenReturn(Optional.of(stockA));
        when(productStockRepository.findByIdForUpdate(PRODUCT_B)).thenReturn(Optional.of(stockB));

        service.reserveStock(eventId, orderId, UUID.randomUUID(), ITEMS_JSON, orderId);

        var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("RESERVED");
        assertThat(captor.getValue().getItems()).hasSize(2);

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("inventory.reserved.v1");

        verify(processedEventRepository).save(any(ProcessedEvent.class));
        // Verifica que o estoque foi decrementado
        assertThat(stockA.getAvailableQty()).isEqualTo(98);
        assertThat(stockA.getReservedQty()).isEqualTo(2);
        assertThat(stockB.getAvailableQty()).isEqualTo(49);
    }

    @Test
    void deveFalharQuandoEstoqueInsuficiente() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();
        var stockA  = ProductStock.of(PRODUCT_A, 1); // só 1 disponível, precisa de 2

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(productStockRepository.findByIdForUpdate(PRODUCT_A)).thenReturn(Optional.of(stockA));

        service.reserveStock(eventId, orderId, UUID.randomUUID(), ITEMS_JSON, orderId);

        var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getFailReason()).contains("insuficiente");

        var outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo("inventory.failed.v1");

        // Estoque não deve ter sido alterado
        assertThat(stockA.getAvailableQty()).isEqualTo(1);
    }

    @Test
    void deveFalharQuandoProdutoNaoExisteNoEstoque() {
        var eventId = UUID.randomUUID();
        var orderId = UUID.randomUUID();

        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(productStockRepository.findByIdForUpdate(PRODUCT_A)).thenReturn(Optional.empty());

        service.reserveStock(eventId, orderId, UUID.randomUUID(), ITEMS_JSON, orderId);

        var captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
    }

    @Test
    void deveIgnorarEventoJaProcessado() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(true);

        service.reserveStock(eventId, UUID.randomUUID(), UUID.randomUUID(),
                ITEMS_JSON, UUID.randomUUID());

        verifyNoInteractions(productStockRepository, reservationRepository, outboxEventRepository);
    }

    @Test
    void deveLiberarLocksAposFalha() {
        var eventId = UUID.randomUUID();
        when(processedEventRepository.existsById(eventId)).thenReturn(false);
        when(productStockRepository.findByIdForUpdate(PRODUCT_A))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
                service.reserveStock(eventId, UUID.randomUUID(), UUID.randomUUID(),
                        ITEMS_JSON, UUID.randomUUID()))
                .isInstanceOf(RuntimeException.class);

        // Locks devem ser liberados no finally
        verify(redisTemplate, atLeastOnce()).delete(anyString());
    }

    // ── findStock ─────────────────────────────────────────────────────────────

    @Test
    void deveLancar404QuandoProdutoNaoExisteNoInventario() {
        var productId = UUID.randomUUID();
        when(productStockRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findStock(productId))
                .isInstanceOf(ProductStockNotFoundException.class);
    }

    // ── addStock ──────────────────────────────────────────────────────────────

    @Test
    void addStock_produtoNovo_criaCadastroDeEstoque() {
        var productId = UUID.randomUUID();
        when(productStockRepository.existsById(productId)).thenReturn(false);
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.addStock(productId, 50);

        assertThat(result.productId()).isEqualTo(productId);
        assertThat(result.availableQty()).isEqualTo(50);
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void addStock_produtoJaExiste_lancaIllegalStateException() {
        var productId = UUID.randomUUID();
        when(productStockRepository.existsById(productId)).thenReturn(true);

        assertThatThrownBy(() -> service.addStock(productId, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já cadastrado");
    }

    // ── updateStock ───────────────────────────────────────────────────────────

    @Test
    void updateStock_produtoExistente_atualizaQtd() {
        var stock = ProductStock.of(PRODUCT_A, 100);
        when(productStockRepository.findById(PRODUCT_A)).thenReturn(Optional.of(stock));
        when(productStockRepository.save(any(ProductStock.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStock(PRODUCT_A, 200);

        assertThat(result.availableQty()).isEqualTo(200);
        verify(productStockRepository).save(any(ProductStock.class));
    }

    @Test
    void updateStock_produtoNaoExiste_lancaNotFoundException() {
        var productId = UUID.randomUUID();
        when(productStockRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStock(productId, 10))
                .isInstanceOf(ProductStockNotFoundException.class);
    }

    // ── findReservation ───────────────────────────────────────────────────────

    @Test
    void deveLancarExcecaoQuandoReservaNaoEncontrada() {
        var orderId = UUID.randomUUID();
        when(reservationRepository.findByOrderIdWithItems(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findReservation(orderId))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}

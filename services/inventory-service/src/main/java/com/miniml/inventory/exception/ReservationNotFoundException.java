package com.miniml.inventory.exception;

import java.util.UUID;

public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(UUID orderId) {
        super("Reserva não encontrada para orderId=" + orderId);
    }
}
